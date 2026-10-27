package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.config.DataSeeder;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.TarefaLimpeza;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoTarefaLimpeza;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.TarefaLimpezaRepository;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import com.gabrielflorencco.hotelimpact.service.HospedeService;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
import com.gabrielflorencco.hotelimpact.service.PagamentoService;
import com.gabrielflorencco.hotelimpact.service.QuartoService;
import com.gabrielflorencco.hotelimpact.service.ReservaService;
import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.dto.ReservaForm;
import com.gabrielflorencco.hotelimpact.dto.PagamentoForm;
import com.gabrielflorencco.hotelimpact.enums.FormaPagamento;
import com.gabrielflorencco.hotelimpact.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Módulo de limpeza (etapa A da evolução do domínio): o check-out abre a
 * tarefa de limpeza do quarto, o início registra a execução e a conclusão
 * integra tarefa e estado do quarto na mesma transação — com o histórico
 * preservado e o índice parcial único (V2) impedindo duas tarefas abertas
 * para o mesmo quarto.
 */
@SpringBootTest(properties = {"app.seed-demo=true"})
class LimpezaIntegracaoTest extends AbstractIntegrationTest {

    @Autowired
    private HospedagemService hospedagemService;
    @Autowired
    private QuartoService quartoService;
    @Autowired
    private ReservaService reservaService;
    @Autowired
    private HospedeService hospedeService;
    @Autowired
    private CategoriaQuartoService categoriaService;
    @Autowired
    private PagamentoService pagamentoService;
    @Autowired
    private TarefaLimpezaRepository tarefas;
    @Autowired
    private QuartoRepository quartos;

    /** Fluxo completo: check-out abre tarefa → iniciar → concluir (quarto disponível, histórico preservado). */
    private Quarto fluxoCompleto() {
        CategoriaForm categoria = new CategoriaForm();
        categoria.setNome("Standard Limpeza " + System.nanoTime());
        categoria.setCapacidadeMaxima(2);
        categoria.setQuantidadeCamas(1);
        categoria.setPrecoBase(new BigDecimal("180.00"));
        categoria.setAtivo(true);
        Long categoriaId = categoriaService.salvar(categoria).getId();

        QuartoForm quarto = new QuartoForm();
        quarto.setNumero("L" + System.nanoTime() % 100000);
        quarto.setCategoriaId(categoriaId);
        quarto.setAndar(1);
        quarto.setAtivo(true);
        Long quartoId = quartoService.salvar(quarto).getId();

        HospedeForm hospede = new HospedeForm();
        hospede.setNome("Hóspede Limpeza");
        Long hospedeId = hospedeService.salvar(hospede).getId();

        ReservaForm reserva = new ReservaForm();
        reserva.setHospedeId(hospedeId);
        reserva.setQuartoId(quartoId);
        reserva.setDataCheckIn(LocalDate.now());
        reserva.setDataCheckOut(LocalDate.now().plusDays(1));
        reserva.setQuantidadeHospedes(1);
        reserva.setValorDiaria(new BigDecimal("180.00"));
        Long reservaId = reservaService.criar(reserva).getId();

        hospedagemService.checkIn(reservaId);

        // conta quitada: o check-out normal exige saldo zero (BR-HSP-002)
        Long hospedagemId = hospedagemService.buscarPorReserva(reservaId).orElseThrow().getId();
        PagamentoForm pagamento = new PagamentoForm();
        pagamento.setValor(new BigDecimal("180.00"));
        pagamento.setForma(FormaPagamento.PIX);
        pagamentoService.registrar(hospedagemId, pagamento);

        hospedagemService.checkOut(hospedagemId);
        return quartos.findById(quartoId).orElseThrow();
    }

    @Test
    void checkOutAbreTarefaDeLimpezaPendenteDoQuarto() {
        Quarto quarto = fluxoCompleto();
        assertThat(quarto.getEstado()).isEqualTo(EstadoQuarto.LIMPEZA);

        TarefaLimpeza tarefa = tarefas
                .findFirstByQuartoIdAndEstadoNotOrderByCriadaEmDesc(quarto.getId(), EstadoTarefaLimpeza.CONCLUIDA)
                .orElseThrow();
        assertThat(tarefa.getEstado()).isEqualTo(EstadoTarefaLimpeza.PENDENTE);
        assertThat(tarefa.getConcluidaEm()).isNull();
        assertThat(tarefa.getCriadaEm()).isNotNull();
    }

    @Test
    void iniciarLimpezaColocaTarefaEmExecucaoSemAlterarOQuarto() {
        Quarto quarto = fluxoCompleto();
        quartoService.iniciarLimpeza(quarto.getId());

        TarefaLimpeza tarefa = tarefas
                .findFirstByQuartoIdAndEstadoNotOrderByCriadaEmDesc(quarto.getId(), EstadoTarefaLimpeza.CONCLUIDA)
                .orElseThrow();
        assertThat(tarefa.getEstado()).isEqualTo(EstadoTarefaLimpeza.EM_EXECUCAO);
        assertThat(quartos.findById(quarto.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoQuarto.LIMPEZA); // o quarto continua em limpeza
    }

    @Test
    void concluirLimpezaConcluiTarefaEDisponibilizaOQuartoPreservandoHistorico() {
        Quarto quarto = fluxoCompleto();
        quartoService.iniciarLimpeza(quarto.getId());
        quartoService.concluirLimpeza(quarto.getId());

        assertThat(quartos.findById(quarto.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoQuarto.DISPONIVEL);

        // histórico preservado: tarefa concluída com data, consultável
        var concluidas = tarefas.findByQuartoIdOrderByCriadaEmDesc(quarto.getId());
        assertThat(concluidas).isNotEmpty();
        TarefaLimpeza historico = concluidas.get(0);
        assertThat(historico.getEstado()).isEqualTo(EstadoTarefaLimpeza.CONCLUIDA);
        assertThat(historico.getConcluidaEm()).isNotNull();

        // nova conclusão de tarefa já concluída não existe: sem tarefa aberta restante
        assertThat(tarefas.findFirstByQuartoIdAndEstadoNotOrderByCriadaEmDesc(
                quarto.getId(), EstadoTarefaLimpeza.CONCLUIDA)).isEmpty();
    }

    @Test
    void iniciarLimpezaExigeTarefaAbertaEQuartoEmLimpeza() {
        Quarto quarto = fluxoCompleto();
        quartoService.iniciarLimpeza(quarto.getId());

        // iniciar de novo: a tarefa aberta está EM_EXECUCAO e a própria
        // entidade recusa a transição (máquina de estados da tarefa)
        assertThatThrownBy(() -> quartoService.iniciarLimpeza(quarto.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Somente tarefas de limpeza pendentes podem ser iniciadas.");

        // quarto fora de limpeza não inicia tarefa
        Quarto disponivel = quartos.findAll().stream()
                .filter(q -> q.getEstado() == EstadoQuarto.DISPONIVEL)
                .findFirst().orElseThrow();
        assertThatThrownBy(() -> quartoService.iniciarLimpeza(disponivel.getId()))
                .hasMessage("Somente quartos aguardando limpeza têm tarefa de limpeza.");
    }

    @Test
    void seedExpoeTarefaAbertaDoQuarto104ParaODashboard() {
        Quarto quarto104 = quartos.findAll().stream()
                .filter(q -> q.getNumero().equals("104"))
                .findFirst().orElseThrow();
        assertThat(quarto104.getEstado()).isEqualTo(EstadoQuarto.LIMPEZA);
        assertThat(tarefas.findFirstByQuartoIdAndEstadoNotOrderByCriadaEmDesc(
                quarto104.getId(), EstadoTarefaLimpeza.CONCLUIDA)).isPresent();
        assertThat(tarefas.countByEstado(EstadoTarefaLimpeza.PENDENTE)).isGreaterThanOrEqualTo(1);
    }
}
