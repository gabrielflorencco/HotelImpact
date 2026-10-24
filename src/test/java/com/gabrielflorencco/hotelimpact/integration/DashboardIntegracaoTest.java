package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.config.DataSeeder;
import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import com.gabrielflorencco.hotelimpact.dto.PagamentoForm;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.dto.ReservaForm;
import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.enums.FormaPagamento;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import com.gabrielflorencco.hotelimpact.service.DashboardService;
import com.gabrielflorencco.hotelimpact.service.HospedeService;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
import com.gabrielflorencco.hotelimpact.service.PagamentoService;
import com.gabrielflorencco.hotelimpact.service.QuartoService;
import com.gabrielflorencco.hotelimpact.service.ReservaService;
import com.gabrielflorencco.hotelimpact.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Indicadores do painel contra PostgreSQL real (Fase 6): valida as consultas
 * de contagem e a soma de receita. Como o container é compartilhado com os
 * demais testes, as asserções usam valores mínimos controlados por este
 * teste (deltas), nunca totais globais.
 */
@SpringBootTest(properties = "app.seed-demo=true")
class DashboardIntegracaoTest extends AbstractIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private QuartoService quartoService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private HospedagemService hospedagemService;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private CategoriaQuartoService categoriaService;

    @Autowired
    private HospedeService hospedeService;

    @Autowired
    private ItemCatalogoRepository itens;

    @Test
    void indicadoresRefletuemDadosCriadosPeloTeste() {
        // Quarto próprio: interditado por este teste conta no indicador.
        Quarto quarto = novoQuartoInterditado("Painel Interditado", "1050");
        var antes = dashboardService.indicadores();

        assertThat(antes.quartosInterditados())
                .isGreaterThanOrEqualTo(1);

        // Chegada prevista hoje: reserva ativa começando hoje.
        Hospede hospede = novoHospede("Hóspede Painel 1050", 3950);
        ReservaForm reservaForm = new ReservaForm();
        reservaForm.setHospedeId(hospede.getId());
        reservaForm.setQuartoId(quarto.getId());
        // Reserva não pode ser criada em quarto interditado; usa outro quarto.
        Quarto livre = novoQuarto("Painel Livre", "1051");
        reservaForm.setQuartoId(livre.getId());
        reservaForm.setDataCheckIn(LocalDate.now());
        reservaForm.setDataCheckOut(LocalDate.now().plusDays(2));
        reservaForm.setQuantidadeHospedes(1);
        reservaForm.setValorDiaria(new BigDecimal("200.00"));
        reservaService.criar(reservaForm);

        var depois = dashboardService.indicadores();
        assertThat(depois.chegadasPrevistasHoje())
                .isGreaterThanOrEqualTo(antes.chegadasPrevistasHoje() + 1);

        // Lista de chegadas de hoje traz a reserva criada acima (com hóspede/quarto).
        assertThat(dashboardService.chegadasDeHoje())
                .anySatisfy(r -> {
                    assertThat(r.getHospede().getNome()).isEqualTo("Hóspede Painel 1050");
                    assertThat(r.getQuarto().getNumero()).isEqualTo("1051");
                });

        // Receita do dia: pagamento confirmado agora entra na soma do dia.
        HospedagemContexto conta = hospedagemComConta("Painel Receita", "1052");
        pagamentoService.registrar(conta.hospedagemId(),
                pagamento("987.65", FormaPagamento.DINHEIRO));

        assertThat(dashboardService.indicadores().receitaConfirmadaHoje())
                .isGreaterThanOrEqualTo(new BigDecimal("987.65"));
    }

    @Test
    void saidasDeHojeTrazemReservasAtivasComCheckOutPrevistoParaHoje() {
        Hospede hospede = novoHospede("Hóspede Saída Painel", 3952);
        Quarto quarto = novoQuarto("Painel Saída", "1053");

        ReservaForm saidaHoje = new ReservaForm();
        saidaHoje.setHospedeId(hospede.getId());
        saidaHoje.setQuartoId(quarto.getId());
        saidaHoje.setDataCheckIn(LocalDate.now().minusDays(2));
        saidaHoje.setDataCheckOut(LocalDate.now());
        saidaHoje.setQuantidadeHospedes(1);
        saidaHoje.setValorDiaria(new BigDecimal("200.00"));
        saidaHoje.setMotivoExcecao("Período iniciado ontem para teste de saídas do painel.");
        reservaService.criar(saidaHoje);

        assertThat(dashboardService.saidasDeHoje())
                .anySatisfy(r -> {
                    assertThat(r.getHospede().getNome()).isEqualTo("Hóspede Saída Painel");
                    assertThat(r.getQuarto().getNumero()).isEqualTo("1053");
                });
    }

    @Test
    void estoqueBaixoContadoPelaConsultaDeCatalogo() {
        ItemCatalogo item = new ItemCatalogo();
        item.setNome("Item Estoque Baixo Painel " + System.nanoTime());
        item.setTipo(TipoItem.PRODUTO);
        item.setPrecoUnitario(new BigDecimal("9.00"));
        item.setControlaEstoque(true);
        item.setEstoqueAtual(3);
        item.setEstoqueMinimo(5);
        item.setAtivo(true);
        itens.save(item);

        assertThat(dashboardService.indicadores().itensComEstoqueBaixo())
                .isGreaterThanOrEqualTo(1);
    }

    // ---- Apoio --------------------------------------------------------------------------

    private Quarto novoQuarto(String nome, String numero) {
        CategoriaForm categoriaForm = new CategoriaForm();
        categoriaForm.setNome("Categoria Painel " + nome + " " + numero);
        categoriaForm.setDescricao("Categoria de teste");
        categoriaForm.setCapacidadeMaxima(2);
        categoriaForm.setQuantidadeCamas(1);
        categoriaForm.setPrecoBase(new BigDecimal("200.00"));
        categoriaForm.setAtivo(true);
        CategoriaQuarto categoria = categoriaService.salvar(categoriaForm);

        QuartoForm quartoForm = new QuartoForm();
        quartoForm.setNumero(numero);
        quartoForm.setCategoriaId(categoria.getId());
        quartoForm.setAndar(10);
        quartoForm.setAtivo(true);
        return quartoService.salvar(quartoForm);
    }

    private Quarto novoQuartoInterditado(String nome, String numero) {
        Quarto quarto = novoQuarto(nome, numero);
        quartoService.interditar(quarto.getId());
        return quartoService.buscarPorId(quarto.getId());
    }

    private Hospede novoHospede(String nome, int sementeCpf) {
        HospedeForm hospedeForm = new HospedeForm();
        hospedeForm.setNome(nome);
        hospedeForm.setCpf(DataSeeder.gerarCpfFicticio(sementeCpf));
        return hospedeService.salvar(hospedeForm);
    }

    private HospedagemContexto hospedagemComConta(String nome, String numeroQuarto) {
        Quarto quarto = novoQuarto(nome, numeroQuarto);
        Hospede hospede = novoHospede("Hóspede " + nome, 3951);
        ReservaForm reservaForm = new ReservaForm();
        reservaForm.setHospedeId(hospede.getId());
        reservaForm.setQuartoId(quarto.getId());
        reservaForm.setDataCheckIn(LocalDate.now());
        reservaForm.setDataCheckOut(LocalDate.now().plusDays(2));
        reservaForm.setQuantidadeHospedes(1);
        reservaForm.setValorDiaria(new BigDecimal("200.00"));
        var reserva = reservaService.criar(reservaForm);
        var hospedagem = hospedagemService.checkIn(reserva.getId());
        return new HospedagemContexto(hospedagem.getId());
    }

    private PagamentoForm pagamento(String valor, FormaPagamento forma) {
        PagamentoForm form = new PagamentoForm();
        form.setValor(new BigDecimal(valor));
        form.setForma(forma);
        return form;
    }

    private record HospedagemContexto(Long hospedagemId) {
    }
}
