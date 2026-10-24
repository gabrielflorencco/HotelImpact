package com.gabrielflorencco.hotelimpact.web;

import com.gabrielflorencco.hotelimpact.controller.HomeController;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Fatia web do painel (MockMvc sem banco): KPIs densos, barra de estados dos
 * quartos, listas de chegadas/saídas do dia, alerta de estoque baixo apenas
 * quando existe e o caso sem dados sem alerta.
 */
@WebMvcTest(controllers = HomeController.class)
@WithMockUser
class HomeControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void painelRenderizaIndicadoresListasDoDiaEBarraDeEstados() throws Exception {
        when(dashboardService.indicadores()).thenReturn(new DashboardService.Indicadores(
                12, 4, 5, 1, 1, 1, 5, 3, 2, new BigDecimal("1234.50"), 0));
        when(dashboardService.chegadasDeHoje()).thenReturn(List.of(reservaMock("RES-2026-000123")));
        when(dashboardService.saidasDeHoje()).thenReturn(List.of(reservaMock("RES-2026-000124")));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(content().string(containsString("Hospedagens em andamento")))
                .andExpect(content().string(containsString("Chegadas de hoje")))
                .andExpect(content().string(containsString("R$ 1.234,50")))
                .andExpect(content().string(containsString("Ocupados:")))
                .andExpect(content().string(containsString("RES-2026-000123")))
                .andExpect(content().string(containsString("RES-2026-000124")))
                .andExpect(content().string(not(containsString("Nenhuma chegada prevista"))))
                .andExpect(content().string(not(containsString("estoque no mínimo"))));
    }

    @Test
    void painelSemDadosRenderizaZerosEstadosVaziosESemAlertaDeEstoque() throws Exception {
        when(dashboardService.indicadores()).thenReturn(new DashboardService.Indicadores(
                0, 0, 0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO, 0));
        when(dashboardService.chegadasDeHoje()).thenReturn(List.of());
        when(dashboardService.saidasDeHoje()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("R$ 0,00")))
                .andExpect(content().string(containsString("Nenhuma chegada prevista para hoje")))
                .andExpect(content().string(containsString("Nenhuma saída prevista para hoje")))
                .andExpect(content().string(not(containsString("estoque no mínimo"))));
    }

    @Test
    void alertaDeEstoqueApareceQuandoHaItensNoMinimo() throws Exception {
        when(dashboardService.indicadores()).thenReturn(new DashboardService.Indicadores(
                0, 0, 0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO, 2));
        when(dashboardService.chegadasDeHoje()).thenReturn(List.of());
        when(dashboardService.saidasDeHoje()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("estoque no mínimo")))
                .andExpect(content().string(containsString("Ver catálogo")));
    }

    private Reserva reservaMock(String codigo) {
        Hospede hospede = new Hospede();
        hospede.setId(10L);
        hospede.setNome("Ana Souza");

        Quarto quarto = new Quarto();
        quarto.setId(20L);
        quarto.setNumero("101");

        Reserva reserva = new Reserva();
        reserva.setId(30L);
        reserva.setCodigo(codigo);
        reserva.setHospede(hospede);
        reserva.setQuarto(quarto);
        reserva.setDataCheckIn(LocalDate.now());
        reserva.setDataCheckOut(LocalDate.now().plusDays(2));
        reserva.setQuantidadeHospedes(2);
        reserva.setValorDiaria(new BigDecimal("180.00"));
        reserva.setDesconto(BigDecimal.ZERO);
        reserva.setValorTotal(new BigDecimal("360.00"));
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return reserva;
    }
}
