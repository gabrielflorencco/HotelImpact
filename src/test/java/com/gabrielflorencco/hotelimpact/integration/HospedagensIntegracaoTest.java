package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.config.DataSeeder;
import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.dto.ReservaForm;
import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import com.gabrielflorencco.hotelimpact.service.HospedeService;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
import com.gabrielflorencco.hotelimpact.service.QuartoService;
import com.gabrielflorencco.hotelimpact.service.ReservaService;
import com.gabrielflorencco.hotelimpact.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regras de negócio das hospedagens (Fase 4): elegibilidade de check-in
 * (reserva ativa na data, sem hospedagem anterior, quarto pronto), ocupação
 * e liberação do quarto (OCUPADO → LIMPEZA → DISPONIVEL), check-out que
 * conclui a reserva, no-show que libera o período e a proteção contra
 * cancelamento de reserva com check-in realizado.
 */
@SpringBootTest(properties = "app.seed-demo=true")
class HospedagensIntegracaoTest extends AbstractIntegrationTest {

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
    private QuartoRepository quartos;

    // ---- Check-in -----------------------------------------------------------------

    @Test
    void checkInValidoCriaHospedagemEmAndamentoEOcupaQuarto() {
        Cenario cenario = novoCenario("Check-in Válido", "981");
        Reserva reserva = reservaService.criar(reservaForm(cenario, LocalDate.now(), LocalDate.now().plusDays(2)));

        assertThat(hospedagemService.podeFazerCheckIn(reserva)).isTrue();

        Hospedagem hospedagem = hospedagemService.checkIn(reserva.getId());

        assertThat(hospedagem.getId()).isNotNull();
        assertThat(hospedagem.getEstado()).isEqualTo(EstadoHospedagem.EM_ANDAMENTO);
        assertThat(hospedagem.getCheckInReal()).isNotNull();
        assertThat(hospedagem.getCheckOutReal()).isNull();
        assertThat(quartoService.buscarPorId(cenario.quarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.OCUPADO);
        assertThat(hospedagemService.podeFazerCheckIn(reserva)).isFalse();
    }

    @Test
    void checkInDeReservaPendenteEAceitoSemAlterarEstadoDaReserva() {
        Cenario cenario = novoCenario("Check-in Pendente", "982");
        Reserva reserva = reservaService.criar(reservaForm(cenario, LocalDate.now(), LocalDate.now().plusDays(2)));
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PENDENTE);

        Hospedagem hospedagem = hospedagemService.checkIn(reserva.getId());

        assertThat(hospedagem.getEstado()).isEqualTo(EstadoHospedagem.EM_ANDAMENTO);
        assertThat(reservaService.buscarPorId(reserva.getId()).getEstado()).isEqualTo(EstadoReserva.PENDENTE);
    }

    @Test
    void checkInDeReservaInexistenteGeraRegistroNaoEncontrado() {
        assertThatThrownBy(() -> hospedagemService.checkIn(999_999L))
                .isInstanceOf(RegistroNaoEncontradoException.class)
                .hasMessageContaining("Reserva");
    }

    @Test
    void checkInDeReservaCanceladaEBloqueado() {
        Cenario cenario = novoCenario("Check-in Cancelada", "983");
        Reserva reserva = reservaService.criar(reservaForm(cenario, LocalDate.now(), LocalDate.now().plusDays(2)));
        reservaService.cancelar(reserva.getId());

        assertThatThrownBy(() -> hospedagemService.checkIn(reserva.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("pendentes ou confirmadas");
    }

    @Test
    void checkInAntecipadoEBloqueado() {
        Cenario cenario = novoCenario("Check-in Antecipado", "984");
        Reserva reserva = reservaService.criar(
                reservaForm(cenario, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4)));

        assertThatThrownBy(() -> hospedagemService.checkIn(reserva.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("a partir da data prevista");
    }

    @Test
    void checkInComQuartoInterditadoEBloqueado() {
        Cenario cenario = novoCenario("Check-in Interditado", "985");
        Reserva reserva = reservaService.criar(reservaForm(cenario, LocalDate.now(), LocalDate.now().plusDays(2)));
        quartoService.interditar(cenario.quarto().getId());

        assertThatThrownBy(() -> hospedagemService.checkIn(reserva.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("fora de operação");
    }

    @Test
    void checkInComQuartoAguardandoLimpezaEBloqueado() {
        Cenario cenario = novoCenario("Check-in Limpeza", "986");
        Reserva reserva = reservaService.criar(reservaForm(cenario, LocalDate.now(), LocalDate.now().plusDays(2)));
        Quarto quarto = quartoService.buscarPorId(cenario.quarto().getId());
        quarto.setEstado(EstadoQuarto.LIMPEZA);
        quartos.save(quarto);

        assertThatThrownBy(() -> hospedagemService.checkIn(reserva.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("aguardando limpeza");
    }

    @Test
    void checkInDuplicadoOuEmQuartoOcupadoEBloqueado() {
        Cenario cenario = novoCenario("Check-in Duplicado", "987");
        Reserva primeira = reservaService.criar(reservaForm(cenario, LocalDate.now(), LocalDate.now().plusDays(2)));
        hospedagemService.checkIn(primeira.getId());

        assertThatThrownBy(() -> hospedagemService.checkIn(primeira.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("já possui uma hospedagem");

        Reserva segunda = reservaService.criar(
                reservaForm(cenario, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7)));
        assertThatThrownBy(() -> hospedagemService.checkIn(segunda.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("ocupado por outra hospedagem");
    }

    // ---- Check-out e ciclo de limpeza ----------------------------------------------

    @Test
    void checkOutEncerraHospedagemConcluiReservaEQuartoFicaEmLimpeza() {
        Cenario cenario = novoCenario("Check-out", "988");
        Reserva reserva = reservaService.criar(reservaForm(cenario, LocalDate.now(), LocalDate.now().plusDays(2)));
        Hospedagem hospedagem = hospedagemService.checkIn(reserva.getId());

        hospedagemService.checkOut(hospedagem.getId());

        Hospedagem encerrada = hospedagemService.buscarPorId(hospedagem.getId());
        assertThat(encerrada.getEstado()).isEqualTo(EstadoHospedagem.ENCERRADA);
        assertThat(encerrada.getCheckOutReal()).isNotNull();
        assertThat(reservaService.buscarPorId(reserva.getId()).getEstado()).isEqualTo(EstadoReserva.CONCLUIDA);
        assertThat(quartoService.buscarPorId(cenario.quarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.LIMPEZA);

        assertThatThrownBy(() -> hospedagemService.checkOut(hospedagem.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("em andamento");
    }

    @Test
    void concluirLimpezaRetornaQuartoADisponivelEForaDeEstadoBloqueia() {
        Cenario cenario = novoCenario("Limpeza Concluída", "989");
        Reserva reserva = reservaService.criar(reservaForm(cenario, LocalDate.now(), LocalDate.now().plusDays(2)));
        Hospedagem hospedagem = hospedagemService.checkIn(reserva.getId());
        hospedagemService.checkOut(hospedagem.getId());

        quartoService.concluirLimpeza(cenario.quarto().getId());
        assertThat(quartoService.buscarPorId(cenario.quarto().getId()).getEstado())
                .isEqualTo(EstadoQuarto.DISPONIVEL);

        assertThatThrownBy(() -> quartoService.concluirLimpeza(cenario.quarto().getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("aguardando limpeza");
    }

    // ---- No-show e proteção de cancelamento -----------------------------------------

    @Test
    void noShowMarcaReservaELiberaPeriodoParaNovaReserva() {
        Cenario cenario = novoCenario("No-show", "990");
        ReservaForm form = reservaForm(cenario, LocalDate.now().minusDays(3), LocalDate.now().minusDays(1));
        form.setMotivoExcecao("Lançamento retroativo para teste de no-show.");
        Reserva reserva = reservaService.criar(form);

        assertThat(reservaService.podeRegistrarNoShow(reserva)).isTrue();
        reservaService.registrarNoShow(reserva.getId());
        assertThat(reservaService.buscarPorId(reserva.getId()).getEstado()).isEqualTo(EstadoReserva.NO_SHOW);

        ReservaForm novaForm = reservaForm(cenario, LocalDate.now().minusDays(3), LocalDate.now().minusDays(1));
        novaForm.setMotivoExcecao("Relançamento retroativo após no-show.");
        Reserva nova = reservaService.criar(novaForm);
        assertThat(nova.getId()).isNotNull();

        assertThatThrownBy(() -> reservaService.registrarNoShow(reserva.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("pendentes ou confirmadas");
    }

    @Test
    void noShowDeReservaFuturaEBloqueado() {
        Cenario cenario = novoCenario("No-show Futuro", "991");
        Reserva reserva = reservaService.criar(
                reservaForm(cenario, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4)));

        assertThat(reservaService.podeRegistrarNoShow(reserva)).isFalse();
        assertThatThrownBy(() -> reservaService.registrarNoShow(reserva.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("após a data prevista");
    }

    @Test
    void noShowOuCancelamentoDeReservaComCheckInEBloqueado() {
        Cenario cenario = novoCenario("Proteção Check-in", "992");
        ReservaForm form = reservaForm(cenario, LocalDate.now().minusDays(1), LocalDate.now().plusDays(2));
        form.setMotivoExcecao("Período iniciado ontem para teste de proteção.");
        Reserva reserva = reservaService.criar(form);
        hospedagemService.checkIn(reserva.getId());

        assertThat(reservaService.podeRegistrarNoShow(reserva)).isFalse();
        assertThatThrownBy(() -> reservaService.registrarNoShow(reserva.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("possui check-in");

        assertThatThrownBy(() -> reservaService.cancelar(reserva.getId()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("check-in já foi realizado");
    }

    // ---- Listagem ---------------------------------------------------------------------

    @Test
    void listagemFiltraPorEstadoEExibeDadosDeReservaHospedeEQuarto() {
        Cenario aberta = novoCenario("Listagem Aberta", "993");
        Reserva reservaAberta = reservaService.criar(
                reservaForm(aberta, LocalDate.now(), LocalDate.now().plusDays(2)));
        Hospedagem emAndamento = hospedagemService.checkIn(reservaAberta.getId());

        Cenario encerrada = novoCenario("Listagem Encerrada", "994");
        Reserva reservaEncerrada = reservaService.criar(
                reservaForm(encerrada, LocalDate.now(), LocalDate.now().plusDays(2)));
        Hospedagem hospedagemEncerrada = hospedagemService.checkIn(reservaEncerrada.getId());
        hospedagemService.checkOut(hospedagemEncerrada.getId());

        assertThat(hospedagemService.buscar(EstadoHospedagem.EM_ANDAMENTO, PageRequest.of(0, 50)).getContent())
                .extracting(Hospedagem::getId)
                .contains(emAndamento.getId())
                .doesNotContain(hospedagemEncerrada.getId());

        assertThat(hospedagemService.buscar(EstadoHospedagem.ENCERRADA, PageRequest.of(0, 50)).getContent())
                .extracting(Hospedagem::getId)
                .contains(hospedagemEncerrada.getId())
                .doesNotContain(emAndamento.getId());

        Hospedagem carregada = hospedagemService.buscarPorId(emAndamento.getId());
        assertThat(carregada.getReserva().getCodigo()).isEqualTo(reservaAberta.getCodigo());
        assertThat(carregada.getReserva().getHospede().getNome()).isEqualTo(aberta.hospede().getNome());
        assertThat(carregada.getQuarto().getNumero()).isEqualTo("993");
    }

    // ---- Apoio --------------------------------------------------------------------------

    /** Cenário isolado por teste: categoria própria, quarto próprio e hóspede próprio. */
    private Cenario novoCenario(String nome, String numeroQuarto) {
        CategoriaForm categoriaForm = new CategoriaForm();
        categoriaForm.setNome("Categoria Hospedagens " + nome + " " + numeroQuarto);
        categoriaForm.setDescricao("Categoria de teste");
        categoriaForm.setCapacidadeMaxima(2);
        categoriaForm.setQuantidadeCamas(1);
        categoriaForm.setPrecoBase(new BigDecimal("200.00"));
        categoriaForm.setAtivo(true);
        CategoriaQuarto categoria = categoriaService.salvar(categoriaForm);

        QuartoForm quartoForm = new QuartoForm();
        quartoForm.setNumero(numeroQuarto);
        quartoForm.setCategoriaId(categoria.getId());
        quartoForm.setAndar(9);
        quartoForm.setAtivo(true);
        Quarto quarto = quartoService.salvar(quartoForm);

        HospedeForm hospedeForm = new HospedeForm();
        hospedeForm.setNome("Hóspede Hospedagens " + nome + " " + numeroQuarto);
        hospedeForm.setCpf(DataSeeder.gerarCpfFicticio(3200 + Integer.parseInt(numeroQuarto) - 900));
        Hospede hospede = hospedeService.salvar(hospedeForm);

        return new Cenario(categoria, quarto, hospede);
    }

    private ReservaForm reservaForm(Cenario cenario, LocalDate checkIn, LocalDate checkOut) {
        ReservaForm form = new ReservaForm();
        form.setHospedeId(cenario.hospede().getId());
        form.setQuartoId(cenario.quarto().getId());
        form.setDataCheckIn(checkIn);
        form.setDataCheckOut(checkOut);
        form.setQuantidadeHospedes(1);
        form.setValorDiaria(new BigDecimal("200.00"));
        return form;
    }

    private record Cenario(CategoriaQuarto categoria, Quarto quarto, Hospede hospede) {
    }
}
