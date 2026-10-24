package com.gabrielflorencco.hotelimpact.web;

import com.gabrielflorencco.hotelimpact.config.SecurityConfig;
import com.gabrielflorencco.hotelimpact.controller.ReservaController;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
import com.gabrielflorencco.hotelimpact.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Navegação SPA (htmx): requisições com header HX-Request recebem apenas o
 * fragmento "conteudo" (sem sidebar/topbar) e o header HX-Push-Url com a URL
 * da página; sessão expirada em pedido htmx responde HX-Redirect ao login
 * em vez de redirecionar a resposta.
 */
@WebMvcTest(controllers = ReservaController.class)
@Import(SecurityConfig.class)
class SpaWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private HospedagemService hospedagemService;

    @MockitoBean
    private com.gabrielflorencco.hotelimpact.repository.HospedeRepository hospedes;

    @MockitoBean
    private com.gabrielflorencco.hotelimpact.repository.QuartoRepository quartos;

    @Test
    @WithMockUser
    void pedidoHtmxRenderizaApenasOFragmentoComHeaderDeUrl() throws Exception {
        when(reservaService.buscar(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/reservas").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/lista :: conteudo"))
                .andExpect(header().string("HX-Push-Url", "/reservas"))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("sidebar"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("app-wrapper"))));
    }

    @Test
    @WithMockUser
    void pedidoSemHtmxRenderizaAPaginaCompleta() throws Exception {
        when(reservaService.buscar(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/reservas"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/lista"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("app-wrapper")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sidebar")));
    }

    @Test
    void sessaoExpiradaComHtmxRespondeHxRedirectEmVezDePagina() throws Exception {
        mockMvc.perform(get("/reservas").header("HX-Request", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("HX-Redirect", "/login"));
    }

    @Test
    void sessaoExpiradaSemHtmxRedirecionaNormalmente() throws Exception {
        mockMvc.perform(get("/reservas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().doesNotExist("HX-Redirect"));
    }
}
