package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.config.DataSeeder;
import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.dto.ReservaForm;
import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import com.gabrielflorencco.hotelimpact.service.HospedeService;
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
 * Regras de negócio das reservas (Fase 4): geração de código sequencial,
 * cálculo do total no servidor, disponibilidade por período semiaberto,
 * capacidade efetiva, exceção administrativa para datas passadas,
 * limite do desconto e transições de estado (confirmar/cancelar).
 */
@SpringBootTest(properties = "app.seed-demo=true")
class ReservasIntegracaoTest extends AbstractIntegrationTest {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private CategoriaQuartoService categoriaService;

    @Autowired
    private QuartoService quartoService;

    @Autowired
    private HospedeService hospedeService;

    @Autowired
    private QuartoRepository quartos;

    @Autowired
    private ReservaRepository reservas;

    // ---- Criação: código, estado inicial e total calculado ----------------------

    @Test
    void criacaoGeraCodigoSequencialEstadoPendenteETotalCalculado() {
        Reserva reserva = reservaService.criar(formularioReserva(novoCenario("Criação", "951")));

        assertThat(reserva.getCodigo()).startsWith("RES-" + LocalDate.now().getYear() + "-")
                .matches("RES-\\d{4}-\\d{6}");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PENDENTE);
        assertThat(reserva.getQuantidadeNoites()).isEqualTo(2);
        assertThat(reserva.getValorBruto()).isEqualByComparingTo("400.00");
        assertThat(reserva.getValorTotal()).isEqualByComparingTo("400.00");
        assertThat(reserva.getDesconto()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void segundaReservaRecebeCodigoComSequenciaSuperior() {
        Cenario cenario = novoCenario("Sequência", "952");
        Reserva primeira = reservaService.criar(formularioReserva(cenario));

        // Período adjacente: mesmo quarto, sem conflito com a primeira reserva.
        ReservaForm segundaForm = formularioReserva(cenario);
        segundaForm.setDataCheckIn(cenario.checkOut());
        segundaForm.setDataCheckOut(cenario.checkOut().plusDays(2));
        Reserva segunda = reservaService.criar(segundaForm);

        assertThat(sequenciaDe(segunda)).isGreaterThan(sequenciaDe(primeira));
    }

    @Test
    void checkOutAnteriorOuIgualAoCheckInEBloqueado() {
        ReservaForm mesmoDia = formularioReserva(novoCenario("Período", "953"));
        mesmoDia.setDataCheckOut(mesmoDia.getDataCheckIn());
        assertThatThrownBy(() -> reservaService.criar(mesmoDia))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("posterior");

        ReservaForm invertido = formularioReserva(novoCenario("Período Invertido", "954"));
        invertido.setDataCheckIn(invertido.getDataCheckOut().plusDays(1));
        assertThatThrownBy(() -> reservaService.criar(invertido))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("posterior");
    }

    // ---- Disponibilidade do quarto ----------------------------------------------

    @Test
    void reservaSobrepostaEBloqueadaMasPeriodoAdjacenteEAceito() {
        Cenario cenario = novoCenario("Sobreposição", "955");
        reservaService.criar(formularioReserva(cenario));

        ReservaForm sobreposta = formularioReserva(cenario);
        sobreposta.setDataCheckIn(cenario.checkIn().plusDays(1));
        sobreposta.setDataCheckOut(cenario.checkOut().plusDays(1));
        assertThatThrownBy(() -> reservaService.criar(sobreposta))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("reserva ativa");

        ReservaForm adjacente = formularioReserva(cenario);
        adjacente.setDataCheckIn(cenario.checkOut());
        adjacente.setDataCheckOut(cenario.checkOut().plusDays(2));
        assertThat(reservaService.criar(adjacente).getId()).isNotNull();
    }

    @Test
    void reservaCanceladaLiberarPeriodoParaNovaReserva() {
        Cenario cenario = novoCenario("Liberação", "956");
        Reserva original = reservaService.criar(formularioReserva(cenario));
        reservaService.cancelar(original.getId());
        assertThat(reservaService.buscarPorId(original.getId()).getEstado()).isEqualTo(EstadoReserva.CANCELADA);

        Reserva nova = reservaService.criar(formularioReserva(cenario));
        assertThat(nova.getId()).isNotNull();
        assertThat(nova.getId()).isNotEqualTo(original.getId());
    }

    @Test
    void quartoInterditadoOuDesativadoNaoRecebeReserva() {
        Cenario interdicao = novoCenario("Interdição", "957");
        quartoService.interditar(interdicao.quarto().getId());
        assertThatThrownBy(() -> reservaService.criar(formularioReserva(interdicao)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("fora de operação");

        Cenario desativacao = novoCenario("Desativação", "958");
        quartoService.alternarAtivo(desativacao.quarto().getId());
        assertThatThrownBy(() -> reservaService.criar(formularioReserva(desativacao)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("desativado");
    }

    // ---- Capacidade e valores -----------------------------------------------------

    @Test
    void capacidadeEfetivaDaCategoriaOuDoQuartoEBedidaNaCriacao() {
        Cenario cenario = novoCenario("Capacidade", "959");

        ReservaForm acimaDaCategoria = formularioReserva(cenario);
        acimaDaCategoria.setQuantidadeHospedes(3);
        assertThatThrownBy(() -> reservaService.criar(acimaDaCategoria))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Capacidade");

        QuartoForm restrito = new QuartoForm();
        restrito.setNumero("960");
        restrito.setCategoriaId(cenario.categoria().getId());
        restrito.setAndar(9);
        restrito.setCapacidadeOperacional(1);
        restrito.setAtivo(true);
        quartoService.salvar(restrito);

        ReservaForm acimaDoQuarto = formularioReserva(cenario);
        acimaDoQuarto.setQuartoId(quartos.findByNumero("960").orElseThrow().getId());
        acimaDoQuarto.setQuantidadeHospedes(2);
        assertThatThrownBy(() -> reservaService.criar(acimaDoQuarto))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Capacidade");

        ReservaForm dentroDoLimite = formularioReserva(cenario);
        dentroDoLimite.setQuartoId(quartos.findByNumero("960").orElseThrow().getId());
        dentroDoLimite.setQuantidadeHospedes(1);
        assertThat(reservaService.criar(dentroDoLimite).getId()).isNotNull();
    }

    @Test
    void descontoAcimaDoBrutoEBloqueadoMasIgualAoBrutoEAceito() {
        ReservaForm abusivo = formularioReserva(novoCenario("Desconto", "961"));
        abusivo.setDesconto(new BigDecimal("400.01"));
        assertThatThrownBy(() -> reservaService.criar(abusivo))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("desconto");

        ReservaForm totalZero = formularioReserva(novoCenario("Desconto Total", "962"));
        totalZero.setDesconto(new BigDecimal("400.00"));
        Reserva reserva = reservaService.criar(totalZero);
        assertThat(reserva.getValorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---- Exceção administrativa e registros inexistentes ------------------------

    @Test
    void reservaComDataPassadaExigeJustificativaDeExcecao() {
        ReservaForm semJustificativa = formularioReserva(novoCenario("Exceção", "963"));
        semJustificativa.setDataCheckIn(LocalDate.now().minusDays(5));
        semJustificativa.setDataCheckOut(LocalDate.now().minusDays(3));
        assertThatThrownBy(() -> reservaService.criar(semJustificativa))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("exceção");

        ReservaForm comJustificativa = formularioReserva(novoCenario("Exceção Justificada", "964"));
        comJustificativa.setDataCheckIn(LocalDate.now().minusDays(5));
        comJustificativa.setDataCheckOut(LocalDate.now().minusDays(3));
        comJustificativa.setMotivoExcecao("Lançamento retroativo de hospedagem já ocorrida.");
        Reserva reserva = reservaService.criar(comJustificativa);
        assertThat(reserva.getMotivoExcecao()).isEqualTo("Lançamento retroativo de hospedagem já ocorrida.");
    }

    @Test
    void hospedeOuQuartoInexistenteGeraRegistroNaoEncontrado() {
        ReservaForm hospedeInexistente = formularioReserva(novoCenario("Inexistente H", "965"));
        hospedeInexistente.setHospedeId(999_999L);
        assertThatThrownBy(() -> reservaService.criar(hospedeInexistente))
                .isInstanceOf(RegistroNaoEncontradoException.class)
                .hasMessageContaining("Hóspede");

        ReservaForm quartoInexistente = formularioReserva(novoCenario("Inexistente Q", "966"));
        quartoInexistente.setQuartoId(999_999L);
        assertThatThrownBy(() -> reservaService.criar(quartoInexistente))
                .isInstanceOf(RegistroNaoEncontradoException.class)
                .hasMessageContaining("Quarto");
    }

    // ---- Transições de estado -------------------------------------------------------

    @Test
    void confirmarECancelarTransicionamEstadosComValidacao() {
        Cenario cenario = novoCenario("Transições", "967");
        Long id = reservaService.criar(formularioReserva(cenario)).getId();
        assertThat(reservaService.buscarPorId(id).getEstado()).isEqualTo(EstadoReserva.PENDENTE);

        reservaService.confirmar(id);
        assertThat(reservaService.buscarPorId(id).getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);

        reservaService.cancelar(id);
        assertThat(reservaService.buscarPorId(id).getEstado()).isEqualTo(EstadoReserva.CANCELADA);

        assertThatThrownBy(() -> reservaService.confirmar(id))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("pendentes");
        assertThatThrownBy(() -> reservaService.cancelar(id))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("pendentes ou confirmadas");
    }

    // ---- Listagem -------------------------------------------------------------------

    @Test
    void listagemFiltraPorEstadoEExibeDadosDeHospedeEQuarto() {
        Cenario mantida = novoCenario("Listagem Mantida", "968");
        Reserva pendente = reservaService.criar(formularioReserva(mantida));

        Cenario cancelada = novoCenario("Listagem Cancelada", "969");
        Reserva outra = reservaService.criar(formularioReserva(cancelada));
        reservaService.cancelar(outra.getId());

        assertThat(reservaService.buscar(null, PageRequest.of(0, 50)).getContent())
                .extracting(Reserva::getCodigo)
                .contains(pendente.getCodigo(), outra.getCodigo());

        assertThat(reservaService.buscar(EstadoReserva.PENDENTE, PageRequest.of(0, 50)).getContent())
                .extracting(Reserva::getCodigo)
                .contains(pendente.getCodigo())
                .doesNotContain(outra.getCodigo());

        assertThat(reservaService.buscar(EstadoReserva.CANCELADA, PageRequest.of(0, 50)).getContent())
                .extracting(Reserva::getCodigo)
                .contains(outra.getCodigo())
                .doesNotContain(pendente.getCodigo());

        Reserva carregada = reservas.findByIdComDetalhes(pendente.getId()).orElseThrow();
        assertThat(carregada.getHospede().getNome()).isEqualTo(mantida.hospede().getNome());
        assertThat(carregada.getQuarto().getNumero()).isEqualTo("968");
    }

    // ---- Apoio ------------------------------------------------------------------------

    /** Cenário isolado por teste: categoria própria, quarto próprio e hóspede próprio. */
    private Cenario novoCenario(String nome, String numeroQuarto) {
        CategoriaForm categoriaForm = new CategoriaForm();
        categoriaForm.setNome("Categoria Reservas " + nome + " " + numeroQuarto);
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
        hospedeForm.setNome("Hóspede Reservas " + nome + " " + numeroQuarto);
        hospedeForm.setCpf(DataSeeder.gerarCpfFicticio(3100 + Integer.parseInt(numeroQuarto) - 900));
        Hospede hospede = hospedeService.salvar(hospedeForm);

        return new Cenario(categoria, quarto, hospede, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));
    }

    private ReservaForm formularioReserva(Cenario cenario) {
        ReservaForm form = new ReservaForm();
        form.setHospedeId(cenario.hospede().getId());
        form.setQuartoId(cenario.quarto().getId());
        form.setDataCheckIn(cenario.checkIn());
        form.setDataCheckOut(cenario.checkOut());
        form.setQuantidadeHospedes(1);
        form.setValorDiaria(new BigDecimal("200.00"));
        return form;
    }

    private long sequenciaDe(Reserva reserva) {
        return Long.parseLong(reserva.getCodigo().substring(reserva.getCodigo().lastIndexOf('-') + 1));
    }

    private record Cenario(CategoriaQuarto categoria, Quarto quarto, Hospede hospede,
                           LocalDate checkIn, LocalDate checkOut) {
    }
}
