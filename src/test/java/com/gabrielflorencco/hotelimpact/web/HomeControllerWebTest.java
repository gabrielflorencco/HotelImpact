package com.gabrielflorencco.hotelimpact.web;

import com.gabrielflorencco.hotelimpact.controller.HomeController;
import com.gabrielflorencco.hotelimpact.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Fatia web do painel (MockMvc sem banco): indicadores renderizados, alerta
 * de estoque baixo apenas quando existe e o caso sem dados sem alerta.
 */
@WebMvcTest(controllers = HomeController.class)
class HomeControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void painelRenderizaIndicadoresReais() throws Exception {
        when(dashboardService.indicadores()).thenReturn(new DashboardService.Indicadores(
                12, 4, 5, 1, 1, 1, 5, 3, 2, new BigDecimal("1234.50"), 0));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(content().string(containsString("Ocupação agora")))
                .andExpect(content().string(containsString("Chegadas de hoje")))
                .andExpect(content().string(containsString("R$ 1.234,50")))
                .andExpect(content().string(containsString("Ocupados:")))
                .andExpect(content().string(containsString("<span>5</span>")))
                .andExpect(content().string(not(containsString("estoque no mínimo"))));
    }

    @Test
    void painelSemDadosRenderizaZerosSemAlertaDeEstoque() throws Exception {
        when(dashboardService.indicadores()).thenReturn(new DashboardService.Indicadores(
                0, 0, 0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO, 0));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("R$ 0,00")))
                .andExpect(content().string(not(containsString("estoque no mínimo"))));
    }

    @Test
    void alertaDeEstoqueApareceQuandoHaItensNoMinimo() throws Exception {
        when(dashboardService.indicadores()).thenReturn(new DashboardService.Indicadores(
                0, 0, 0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO, 2));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("estoque no mínimo")))
                .andExpect(content().string(containsString("Ver catálogo")));
    }
}
