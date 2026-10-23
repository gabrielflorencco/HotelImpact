package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.config.DataSeeder;
import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.dto.ConsumoForm;
import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.dto.ReservaForm;
import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoConsumo;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import com.gabrielflorencco.hotelimpact.service.ConsumoService;
import com.gabrielflorencco.hotelimpact.service.HospedeService;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
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
 * Regras de negócio dos consumos (Fase 5): preço unitário histórico capturado
 * no lançamento, baixa de estoque apenas para produtos com controle efetivo,
 * estoque nunca negativo, cancelamento como estorno lógico que restitui o
 * estoque uma única vez e a conta que soma somente os lançamentos ativos.
 */
@SpringBootTest(properties = "app.seed-demo=true")
class ConsumosIntegracaoTest extends AbstractIntegrationTest {

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

    @Autowired
    private ItemCatalogoRepository itens;

    // ---- Lançamento -----------------------------------------------------------------

    @Test
    void lancarProdutoComEstoqueDebitaECapturaPrecoHistorico() {
        Cenario cenario = novoCenario("Consumo Preço", "995");
        ItemCatalogo agua = novoItem("Água Teste 995", TipoItem.PRODUTO, "5.00", true, 100);
        Hospedagem hospedagem = checkInRealizado(cenario);

        ConsumoForm form = new ConsumoForm();
        form.setItemId(agua.getId());
        form.setQuantidade(2);
        consumoService.lancar(hospedagem.getId(), form);

        ItemCatalogo apos = itens.findById(agua.getId()).orElseThrow();
        assertThat(apos.getEstoqueAtual()).isEqualTo(98);

        // Preço histórico: mudanças no catálogo não afetam lançamentos existentes.
        // (A alteração usa a instância recarregada — a cópia original está defasada
        // após a atualização de versão feita pela baixa de estoque.)
        apos.setPrecoUnitario(new BigDecimal("7.00"));
        itens.save(apos);
        var lancamentos = consumoService.listarPorHospedagem(hospedagem.getId());
        assertThat(lancamentos).hasSize(1);
        assertThat(lancamentos.get(0).getPrecoUnitario()).isEqualByComparingTo("5.00");
        assertThat(lancamentos.get(0).getValorTotal()).isEqualByComparingTo("10.00");
    }

    @Test
    void lancarServicoNuncaMovimentaEstoque() {
        Cenario cenario = novoCenario("Consumo Serviço", "996");
        ItemCatalogo lavanderia = novoItem("Lavanderia Teste 996", TipoItem.SERVICO, "25.00", true, 0);
        Hospedagem hospedagem = checkInRealizado(cenario);

        ConsumoForm form = new ConsumoForm();
        form.setItemId(lavanderia.getId());
        form.setQuantidade(3);
        consumoService.lancar(hospedagem.getId(), form);

        ItemCatalogo apos = itens.findById(lavanderia.getId()).orElseThrow();
        assertThat(apos.getEstoqueAtual()).isZero();
        assertThat(consumoService.totalAtivos(hospedagem.getId())).isEqualByComparingTo("75.00");
    }

    @Test
    void lancarProdutoSemControleDeEstoqueNaoDebita() {
        Cenario cenario = novoCenario("Consumo Sem Controle", "997");
        ItemCatalogo lanche = novoItem("Lanche Teste 997", TipoItem.PRODUTO, "18.00", false, 0);
        Hospedagem hospedagem = checkInRealizado(cenario);

        ConsumoForm form = new ConsumoForm();
        form.setItemId(lanche.getId());
        form.setQuantidade(1);
        consumoService.lancar(hospedagem.getId(), form);

        assertThat(itens.findById(lanche.getId()).orElseThrow().getEstoqueAtual()).isZero();
        assertThat(consumoService.listarPorHospedagem(hospedagem.getId())).hasSize(1);
    }

    @Test
    void lancarComEstoqueInsuficienteEBloqueadoSemPersistir() {
        Cenario cenario = novoCenario("Consumo Estoque", "998");
        ItemCatalogo refrigerante = novoItem("Refrigerante Teste 998", TipoItem.PRODUTO, "7.00", true, 1);
        Hospedagem hospedagem = checkInRealizado(cenario);

        ConsumoForm form = new ConsumoForm();
        form.setItemId(refrigerante.getId());
        form.setQuantidade(2);
        assertThatThrownBy(() -> consumoService.lancar(hospedagem.getId(), form))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Estoque insuficiente");

        assertThat(consumoService.listarPorHospedagem(hospedagem.getId())).isEmpty();
        assertThat(itens.findById(refrigerante.getId()).orElseThrow().getEstoqueAtual()).isEqualTo(1);
    }

    @Test
    void lancarItemInativoEBloqueado() {
        Cenario cenario = novoCenario("Consumo Inativo", "999");
        ItemCatalogo item = novoItem("Item Inativo Teste 999", TipoItem.PRODUTO, "9.00", false, 0);
        item.setAtivo(false);
        itens.save(item);
        Hospedagem hospedagem = checkInRealizado(cenario);

        ConsumoForm form = new ConsumoForm();
        form.setItemId(item.getId());
        form.setQuantidade(1);
        assertThatThrownBy(() -> consumoService.lancar(hospedagem.getId(), form))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void lancarEmHospedagemEncerradaEBloqueado() {
        Cenario cenario = novoCenario("Consumo Encerrada", "1000");
        ItemCatalogo agua = novoItem("Água Teste 1000", TipoItem.PRODUTO, "5.00", false, 0);
        Hospedagem hospedagem = checkInRealizado(cenario);
        hospedagemService.checkOut(hospedagem.getId());

        ConsumoForm form = new ConsumoForm();
        form.setItemId(agua.getId());
        form.setQuantidade(1);
        assertThatThrownBy(() -> consumoService.lancar(hospedagem.getId(), form))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("em andamento");
    }

    @Test
    void lancarEmHospedagemInexistenteGeraRegistroNaoEncontrado() {
        ConsumoForm form = new ConsumoForm();
        form.setItemId(1L);
        form.setQuantidade(1);
        assertThatThrownBy(() -> consumoService.lancar(999_999L, form))
                .isInstanceOf(RegistroNaoEncontradoException.class)
                .hasMessageContaining("Hospedagem");
    }

    // ---- Cancelamento (estorno lógico) -----------------------------------------------

    @Test
    void cancelarRestituiEstoqueExatamenteUmaVez() {
        Cenario cenario = novoCenario("Consumo Cancelar", "1001");
        ItemCatalogo agua = novoItem("Água Teste 1001", TipoItem.PRODUTO, "5.00", true, 10);
        Hospedagem hospedagem = checkInRealizado(cenario);

        ConsumoForm form = new ConsumoForm();
        form.setItemId(agua.getId());
        form.setQuantidade(3);
        var lancado = consumoService.lancar(hospedagem.getId(), form);
        assertThat(itens.findById(agua.getId()).orElseThrow().getEstoqueAtual()).isEqualTo(7);

        consumoService.cancelar(hospedagem.getId(), lancado.getId());
        assertThat(itens.findById(agua.getId()).orElseThrow().getEstoqueAtual()).isEqualTo(10);
        assertThat(consumoService.totalAtivos(hospedagem.getId())).isEqualByComparingTo(BigDecimal.ZERO);

        assertThatThrownBy(() -> consumoService.cancelar(hospedagem.getId(), lancado.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("já está cancelado");
        assertThat(itens.findById(agua.getId()).orElseThrow().getEstoqueAtual()).isEqualTo(10);
    }

    @Test
    void cancelarConsumoDeOutraHospedagemGeraRegistroNaoEncontrado() {
        Cenario cenario = novoCenario("Consumo Outra", "1002");
        ItemCatalogo agua = novoItem("Água Teste 1002", TipoItem.PRODUTO, "5.00", false, 0);
        Hospedagem hospedagem = checkInRealizado(cenario);
        ConsumoForm form = new ConsumoForm();
        form.setItemId(agua.getId());
        form.setQuantidade(1);
        var lancado = consumoService.lancar(hospedagem.getId(), form);

        assertThatThrownBy(() -> consumoService.cancelar(999_999L, lancado.getId()))
                .isInstanceOf(RegistroNaoEncontradoException.class);
    }

    @Test
    void cancelarEmHospedagemEncerradaEBloqueado() {
        Cenario cenario = novoCenario("Consumo Cancelar Encerrada", "1003");
        ItemCatalogo agua = novoItem("Água Teste 1003", TipoItem.PRODUTO, "5.00", false, 0);
        Hospedagem hospedagem = checkInRealizado(cenario);
        ConsumoForm form = new ConsumoForm();
        form.setItemId(agua.getId());
        form.setQuantidade(1);
        var lancado = consumoService.lancar(hospedagem.getId(), form);
        hospedagemService.checkOut(hospedagem.getId());

        assertThatThrownBy(() -> consumoService.cancelar(hospedagem.getId(), lancado.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("em andamento");
    }

    @Test
    void totalAtivosSomaSomenteLancamentosAtivos() {
        Cenario cenario = novoCenario("Consumo Total", "1004");
        ItemCatalogo agua = novoItem("Água Teste 1004", TipoItem.PRODUTO, "5.00", false, 0);
        ItemCatalogo lanche = novoItem("Lanche Teste 1004", TipoItem.PRODUTO, "18.00", false, 0);
        Hospedagem hospedagem = checkInRealizado(cenario);

        ConsumoForm primeira = new ConsumoForm();
        primeira.setItemId(agua.getId());
        primeira.setQuantidade(2);
        var aguaLancada = consumoService.lancar(hospedagem.getId(), primeira);

        ConsumoForm segunda = new ConsumoForm();
        segunda.setItemId(lanche.getId());
        segunda.setQuantidade(1);
        consumoService.lancar(hospedagem.getId(), segunda);

        assertThat(consumoService.totalAtivos(hospedagem.getId())).isEqualByComparingTo("28.00");

        consumoService.cancelar(hospedagem.getId(), aguaLancada.getId());
        assertThat(consumoService.totalAtivos(hospedagem.getId())).isEqualByComparingTo("18.00");

        var lancamentos = consumoService.listarPorHospedagem(hospedagem.getId());
        assertThat(lancamentos).hasSize(2);
        assertThat(lancamentos.stream().filter(c -> c.getEstado() == EstadoConsumo.CANCELADO)).hasSize(1);
    }

    // ---- Apoio ------------------------------------------------------------------------

    private Cenario novoCenario(String nome, String numeroQuarto) {
        CategoriaForm categoriaForm = new CategoriaForm();
        categoriaForm.setNome("Categoria Consumos " + nome + " " + numeroQuarto);
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
        hospedeForm.setNome("Hóspede Consumos " + nome + " " + numeroQuarto);
        hospedeForm.setCpf(DataSeeder.gerarCpfFicticio(3400 + Integer.parseInt(numeroQuarto) - 900));
        Hospede hospede = hospedeService.salvar(hospedeForm);

        return new Cenario(categoria, quarto, hospede);
    }

    private Hospedagem checkInRealizado(Cenario cenario) {
        ReservaForm reservaForm = new ReservaForm();
        reservaForm.setHospedeId(cenario.hospede().getId());
        reservaForm.setQuartoId(cenario.quarto().getId());
        reservaForm.setDataCheckIn(LocalDate.now());
        reservaForm.setDataCheckOut(LocalDate.now().plusDays(2));
        reservaForm.setQuantidadeHospedes(1);
        reservaForm.setValorDiaria(new BigDecimal("200.00"));
        Reserva reserva = reservaService.criar(reservaForm);
        return hospedagemService.checkIn(reserva.getId());
    }

    private ItemCatalogo novoItem(String nome, TipoItem tipo, String preco, boolean controlaEstoque, int estoque) {
        ItemCatalogo item = new ItemCatalogo();
        item.setNome(nome);
        item.setTipo(tipo);
        item.setPrecoUnitario(new BigDecimal(preco));
        item.setControlaEstoque(controlaEstoque);
        item.setEstoqueAtual(estoque);
        item.setEstoqueMinimo(0);
        item.setAtivo(true);
        return itens.save(item);
    }

    private record Cenario(CategoriaQuarto categoria, Quarto quarto, Hospede hospede) {
    }
}
