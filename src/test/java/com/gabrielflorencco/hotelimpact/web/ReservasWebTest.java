package com.gabrielflorencco.hotelimpact.web;

import com.gabrielflorencco.hotelimpact.controller.ReservaController;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.HospedeRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
import com.gabrielflorencco.hotelimpact.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Fatia web das reservas (MockMvc sem banco): renderização das telas,
 * validação de formulário, padrão Post/Redirect/Get, tratamento global de
 * erros e ações de check-in/no-show — os services são simulados.
 */
@WebMvcTest(controllers = ReservaController.class)

@org.springframework.security.test.context.support.WithMockUser
class ReservasWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private HospedagemService hospedagemService;

    @MockitoBean
    private HospedeRepository hospedes;

    @MockitoBean
    private QuartoRepository quartos;

    // ---- Listagem e detalhe ---------------------------------------------------------

    @Test
    void listarRenderizaTabelaComCodigoDaReserva() throws Exception {
        when(reservaService.buscar(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reservaMock(1L, "RES-2026-000123"))));

        mockMvc.perform(get("/reservas"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/lista"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("RES-2026-000123")));
    }

    @Test
    void listarSemReservasExibeEstadoVazio() throws Exception {
        when(reservaService.buscar(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/reservas"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhuma reserva encontrada")));
    }

    @Test
    void detalheExibeBotaoDeCheckInQuandoReservaElegivel() throws Exception {
        Reserva reserva = reservaMock(1L, "RES-2026-000123");
        when(reservaService.buscarPorId(1L)).thenReturn(reserva);
        when(hospedagemService.buscarPorReserva(1L)).thenReturn(Optional.empty());
        when(hospedagemService.podeFazerCheckIn(any(Reserva.class))).thenReturn(true);
        when(reservaService.podeRegistrarNoShow(any(Reserva.class))).thenReturn(false);

        mockMvc.perform(get("/reservas/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fazer check-in")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Registrar no-show"))));
    }

    @Test
    void detalheExibeLinkParaHospedagemExistente() throws Exception {
        Reserva reserva = reservaMock(1L, "RES-2026-000123");
        when(reservaService.buscarPorId(1L)).thenReturn(reserva);
        Hospedagem hospedagem = new Hospedagem();
        hospedagem.setId(7L);
        when(hospedagemService.buscarPorReserva(1L)).thenReturn(Optional.of(hospedagem));
        when(hospedagemService.podeFazerCheckIn(any(Reserva.class))).thenReturn(false);
        when(reservaService.podeRegistrarNoShow(any(Reserva.class))).thenReturn(false);

        mockMvc.perform(get("/reservas/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ver hospedagem")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Fazer check-in"))));
    }

    @Test
    void detalheDeReservaInexistenteRetorna404ComPaginaDeErro() throws Exception {
        when(reservaService.buscarPorId(anyLong()))
                .thenThrow(new RegistroNaoEncontradoException("Reserva não encontrada."));

        mockMvc.perform(get("/reservas/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/erro-generico"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reserva não encontrada")));
    }

    // ---- Criação: validação, negócio e Post/Redirect/Get ------------------------------

    @Test
    void criarComFormularioInvalidoReRenderizaSemChamarOService() throws Exception {
        mockMvc.perform(post("/reservas").with(csrf()).param("hospedeId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/formulario"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Campo obrigatório")));

        verify(reservaService, never()).criar(any());
    }

    @Test
    void criarValidoRedirecionaParaDetalheDaReserva() throws Exception {
        when(reservaService.criar(any())).thenReturn(reservaMock(1L, "RES-2026-000123"));

        mockMvc.perform(post("/reservas").with(csrf())
                        .param("hospedeId", "1")
                        .param("quartoId", "1")
                        .param("dataCheckIn", "2026-11-01")
                        .param("dataCheckOut", "2026-11-03")
                        .param("quantidadeHospedes", "2")
                        .param("valorDiaria", "180.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservas/1"));

        verify(reservaService).criar(any());
    }

    @Test
    void criarComRegraDeNegocioVioladaReRenderizaComMensagemDeErro() throws Exception {
        when(reservaService.criar(any()))
                .thenThrow(new NegocioException("Já existe uma reserva ativa para o quarto 101 no período informado."));

        mockMvc.perform(post("/reservas").with(csrf())
                        .param("hospedeId", "1")
                        .param("quartoId", "1")
                        .param("dataCheckIn", "2026-11-01")
                        .param("dataCheckOut", "2026-11-03")
                        .param("quantidadeHospedes", "2")
                        .param("valorDiaria", "180.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/formulario"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Já existe uma reserva ativa")));
    }

    // ---- Ações de estado: PRG -------------------------------------------------------

    @Test
    void confirmarRedirecionaDeVoltaParaODetalhe() throws Exception {
        mockMvc.perform(post("/reservas/1/confirmar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservas/1"));

        verify(reservaService).confirmar(1L);
    }

    @Test
    void cancelarBloqueadoPorRegraRedirecionaComErroNaQuery() throws Exception {
        org.mockito.Mockito.doThrow(new NegocioException("Não é possível cancelar a reserva: o check-in já foi realizado."))
                .when(reservaService).cancelar(1L);

        mockMvc.perform(post("/reservas/1/cancelar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservas/1"));

        verify(reservaService).cancelar(1L);
    }

    @Test
    void checkInValidoRedirecionaParaDetalheDaHospedagem() throws Exception {
        Hospedagem hospedagem = new Hospedagem();
        hospedagem.setId(7L);
        Quarto quarto = new Quarto();
        quarto.setNumero("101");
        hospedagem.setQuarto(quarto);
        when(hospedagemService.checkIn(1L)).thenReturn(hospedagem);

        mockMvc.perform(post("/reservas/1/check-in").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/hospedagens/7"));

        verify(hospedagemService).checkIn(1L);
    }

    @Test
    void checkInBloqueadoRedirecionaDeVoltaParaAReserva() throws Exception {
        when(hospedagemService.checkIn(1L))
                .thenThrow(new NegocioException("O quarto 101 está aguardando limpeza."));

        mockMvc.perform(post("/reservas/1/check-in").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservas/1"));

        verify(hospedagemService).checkIn(1L);
    }

    @Test
    void registrarNoShowRedirecionaDeVoltaParaAReserva() throws Exception {
        mockMvc.perform(post("/reservas/1/no-show").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservas/1"));

        verify(reservaService).registrarNoShow(1L);
    }

    // ---- Apoio -----------------------------------------------------------------------

    private Reserva reservaMock(Long id, String codigo) {
        Hospede hospede = new Hospede();
        hospede.setId(10L);
        hospede.setNome("Ana Souza");

        Quarto quarto = new Quarto();
        quarto.setId(20L);
        quarto.setNumero("101");

        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setCodigo(codigo);
        reserva.setHospede(hospede);
        reserva.setQuarto(quarto);
        reserva.setDataCheckIn(LocalDate.now());
        reserva.setDataCheckOut(LocalDate.now().plusDays(2));
        reserva.setQuantidadeHospedes(2);
        reserva.setValorDiaria(new BigDecimal("180.00"));
        reserva.setDesconto(BigDecimal.ZERO);
        reserva.setValorTotal(new BigDecimal("360.00"));
        reserva.setEstado(EstadoReserva.PENDENTE);
        reserva.setDataCriacao(LocalDateTime.now());
        return reserva;
    }
}
