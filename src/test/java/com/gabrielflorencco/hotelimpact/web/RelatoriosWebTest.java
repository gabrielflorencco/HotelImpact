package com.gabrielflorencco.hotelimpact.web;

import com.gabrielflorencco.hotelimpact.controller.RelatorioController;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.service.RelatorioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Fatia web dos relatórios (MockMvc sem banco): período padrão do mês,
 * filtros por datas, caso sem dados e a listagem de hospedagens do período.
 */
@WebMvcTest(controllers = RelatorioController.class)
@WithMockUser
class RelatoriosWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioService relatorioService;

    @Test
    void periodoSemDadosRenderizaZerosEEstadoVazio() throws Exception {
        when(relatorioService.resumoPeriodo(any(), any())).thenReturn(new RelatorioService.ResumoPeriodo(
                LocalDate.now().withDayOfMonth(1), LocalDate.now(),
                BigDecimal.ZERO, 0, 0, 0, 0, 0, List.of()));

        mockMvc.perform(get("/relatorios"))
                .andExpect(status().isOk())
                .andExpect(view().name("relatorios/index"))
                .andExpect(content().string(containsString("Nenhuma hospedagem iniciada")))
                .andExpect(content().string(containsString("R$ 0,00")));
    }

    @Test
    void periodoComDadosRenderizaNumerosEHospedagens() throws Exception {
        when(relatorioService.resumoPeriodo(any(), any())).thenReturn(new RelatorioService.ResumoPeriodo(
                LocalDate.now().withDayOfMonth(1), LocalDate.now(),
                new BigDecimal("2500.00"), 7, 5, 12, 2, 1,
                List.of(hospedagemMock())));

        mockMvc.perform(get("/relatorios"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("R$ 2.500,00")))
                .andExpect(content().string(containsString("RES-2026-000123")))
                .andExpect(content().string(containsString("Ana Souza")))
                .andExpect(content().string(containsString("Check-ins")));
    }

    @Test
    void periodoCustomRecebeAsDatasComoParametros() throws Exception {
        when(relatorioService.resumoPeriodo(any(), any())).thenReturn(new RelatorioService.ResumoPeriodo(
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 10),
                BigDecimal.ZERO, 0, 0, 0, 0, 0, List.of()));

        mockMvc.perform(get("/relatorios")
                        .param("inicio", "2026-10-05")
                        .param("fim", "2026-10-10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("05/10/2026")));
    }

    private Hospedagem hospedagemMock() {
        Hospede hospede = new Hospede();
        hospede.setId(10L);
        hospede.setNome("Ana Souza");

        Quarto quarto = new Quarto();
        quarto.setId(20L);
        quarto.setNumero("101");

        Reserva reserva = new Reserva();
        reserva.setId(30L);
        reserva.setCodigo("RES-2026-000123");
        reserva.setHospede(hospede);
        reserva.setQuarto(quarto);
        reserva.setValorDiaria(new BigDecimal("180.00"));
        reserva.setValorTotal(new BigDecimal("360.00"));
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        Hospedagem hospedagem = new Hospedagem();
        hospedagem.setId(40L);
        hospedagem.setReserva(reserva);
        hospedagem.setQuarto(quarto);
        hospedagem.setCheckInReal(LocalDateTime.now().minusHours(2));
        hospedagem.setEstado(EstadoHospedagem.EM_ANDAMENTO);
        return hospedagem;
    }
}
