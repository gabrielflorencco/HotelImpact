package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.config.DataSeeder;
import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.dto.ConsumoForm;
import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import com.gabrielflorencco.hotelimpact.dto.ItemCatalogoForm;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.dto.ReservaForm;
import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import com.gabrielflorencco.hotelimpact.service.ConsumoService;
import com.gabrielflorencco.hotelimpact.service.HospedeService;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
import com.gabrielflorencco.hotelimpact.service.ItemCatalogoService;
import com.gabrielflorencco.hotelimpact.service.QuartoService;
import com.gabrielflorencco.hotelimpact.service.ReservaService;
import com.gabrielflorencco.hotelimpact.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regras de negócio do catálogo de itens (Fase 5): nome único, serviços
 * nunca controlam estoque (a flag é ignorada), edição preserva o estoque
 * debitado por consumos e exclusão bloqueada por histórico de lançamentos.
 */
@SpringBootTest(properties = "app.seed-demo=true")
class ItensIntegracaoTest extends AbstractIntegrationTest {

    @Autowired
    private ItemCatalogoService itemService;

    @Autowired
    private ItemCatalogoRepository itens;

    @Autowired
    private ConsumoService consumoService;

    @Autowired
    private HospedagemService hospedagemService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private QuartoService quartoService;

    @Autowired
    private CategoriaQuartoService categoriaService;

    @Autowired
    private HospedeService hospedeService;

    @Test
    void criarProdutoComControleDeEstoque() {
        ItemCatalogo salvo = itemService.salvar(formItem("Refrigerante Catálogo 1015", TipoItem.PRODUTO,
                "7.00", true, 80, 15, true));

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.isEstoqueControladoEfetivo()).isTrue();
        assertThat(salvo.getEstoqueAtual()).isEqualTo(80);
        assertThat(salvo.getEstoqueMinimo()).isEqualTo(15);
    }

    @Test
    void criarServicoIgnoraFlagDeEstoque() {
        ItemCatalogo salvo = itemService.salvar(formItem("Massagem Catálogo 1016", TipoItem.SERVICO,
                "90.00", true, 50, 10, true));

        assertThat(salvo.getControlaEstoque()).isFalse();
        assertThat(salvo.getEstoqueAtual()).isZero();
        assertThat(salvo.getEstoqueMinimo()).isZero();
        assertThat(salvo.isEstoqueControladoEfetivo()).isFalse();
    }

    @Test
    void nomeDuplicadoIgnorandoMaiusculasEBloqueado() {
        itemService.salvar(formItem("Água Gaseificada Catálogo 1017", TipoItem.PRODUTO, "6.00", false, 0, 0, true));

        ItemCatalogoForm duplicado = formItem("água gaseificada catálogo 1017", TipoItem.PRODUTO,
                "6.50", false, 0, 0, true);
        assertThatThrownBy(() -> itemService.salvar(duplicado))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("nome");
    }

    @Test
    void editarPreservaEstoqueDebitadoPorConsumo() {
        ItemCatalogo item = itemService.salvar(formItem("Lanche Catálogo 1018", TipoItem.PRODUTO,
                "18.00", true, 10, 2, true));
        Hospedagem hospedagem = checkInRealizado("Edição Estoque", "1019");
        ConsumoForm consumo = new ConsumoForm();
        consumo.setItemId(item.getId());
        consumo.setQuantidade(3);
        consumoService.lancar(hospedagem.getId(), consumo);
        assertThat(itens.findById(item.getId()).orElseThrow().getEstoqueAtual()).isEqualTo(7);

        ItemCatalogoForm edicao = ItemCatalogoForm.de(itemService.buscarPorId(item.getId()));
        edicao.setNome("Lanche Especial Catálogo 1018");
        edicao.setPrecoUnitario(new BigDecimal("19.00"));
        itemService.salvar(edicao);

        ItemCatalogo apos = itemService.buscarPorId(item.getId());
        assertThat(apos.getNome()).isEqualTo("Lanche Especial Catálogo 1018");
        assertThat(apos.getPrecoUnitario()).isEqualByComparingTo("19.00");
        assertThat(apos.getEstoqueAtual()).isEqualTo(7);
    }

    @Test
    void excluirItemSemHistoricoPermitido() {
        ItemCatalogo item = itemService.salvar(formItem("Item Livre Catálogo 1020", TipoItem.PRODUTO,
                "3.00", false, 0, 0, true));

        itemService.excluir(item.getId());
        assertThat(itens.existsById(item.getId())).isFalse();
    }

    @Test
    void excluirItemComConsumosBloqueadoPreservandoHistorico() {
        ItemCatalogo item = itemService.salvar(formItem("Item Consumido Catálogo 1021", TipoItem.PRODUTO,
                "5.00", false, 0, 0, true));
        Hospedagem hospedagem = checkInRealizado("Exclusão Com Histórico", "1022");
        ConsumoForm consumo = new ConsumoForm();
        consumo.setItemId(item.getId());
        consumo.setQuantidade(1);
        consumoService.lancar(hospedagem.getId(), consumo);

        assertThatThrownBy(() -> itemService.excluir(item.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("histórico")
                .hasMessageContaining("Desative-o");
        assertThat(itens.existsById(item.getId())).isTrue();
    }

    @Test
    void itemInativoPermaneceNoCatalogoMasSaiDaListaDeLancamento() {
        ItemCatalogo inativo = itemService.salvar(formItem("Item Inativo Catálogo 1023", TipoItem.PRODUTO,
                "4.00", false, 0, 0, false));

        assertThat(itemService.listar()).anyMatch(i -> i.getId().equals(inativo.getId()));
        assertThat(itens.findAllByAtivoTrueOrderByNomeAsc())
                .noneMatch(i -> i.getId().equals(inativo.getId()));
    }

    // ---- Apoio --------------------------------------------------------------------------

    private ItemCatalogoForm formItem(String nome, TipoItem tipo, String preco, boolean controlaEstoque,
                                      int estoque, int minimo, boolean ativo) {
        ItemCatalogoForm form = new ItemCatalogoForm();
        form.setNome(nome);
        form.setTipo(tipo);
        form.setPrecoUnitario(new BigDecimal(preco));
        form.setControlaEstoque(controlaEstoque);
        form.setEstoqueAtual(estoque);
        form.setEstoqueMinimo(minimo);
        form.setAtivo(ativo);
        return form;
    }

    private Hospedagem checkInRealizado(String nome, String numeroQuarto) {
        CategoriaForm categoriaForm = new CategoriaForm();
        categoriaForm.setNome("Categoria Itens " + nome + " " + numeroQuarto);
        categoriaForm.setDescricao("Categoria de teste");
        categoriaForm.setCapacidadeMaxima(2);
        categoriaForm.setQuantidadeCamas(1);
        categoriaForm.setPrecoBase(new BigDecimal("200.00"));
        categoriaForm.setAtivo(true);
        CategoriaQuarto categoria = categoriaService.salvar(categoriaForm);

        QuartoForm quartoForm = new QuartoForm();
        quartoForm.setNumero(numeroQuarto);
        quartoForm.setCategoriaId(categoria.getId());
        quartoForm.setAndar(10);
        quartoForm.setAtivo(true);
        Quarto quarto = quartoService.salvar(quartoForm);

        HospedeForm hospedeForm = new HospedeForm();
        hospedeForm.setNome("Hóspede Itens " + nome + " " + numeroQuarto);
        hospedeForm.setCpf(DataSeeder.gerarCpfFicticio(3800 + Integer.parseInt(numeroQuarto) - 1000));
        Hospede hospede = hospedeService.salvar(hospedeForm);

        ReservaForm reservaForm = new ReservaForm();
        reservaForm.setHospedeId(hospede.getId());
        reservaForm.setQuartoId(quarto.getId());
        reservaForm.setDataCheckIn(LocalDate.now());
        reservaForm.setDataCheckOut(LocalDate.now().plusDays(2));
        reservaForm.setQuantidadeHospedes(1);
        reservaForm.setValorDiaria(new BigDecimal("200.00"));
        Reserva reserva = reservaService.criar(reservaForm);
        return hospedagemService.checkIn(reserva.getId());
    }
}
