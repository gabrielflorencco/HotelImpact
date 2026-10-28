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

    @MockitoBean
    private com.gabrielflorencco.hotelimpact.service.HospedeService hospedeService;

    @MockitoBean
    private com.gabrielflorencco.hotelimpact.service.QuartoService quartoService;

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

        var resposta = mockMvc.perform(get("/reservas"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/lista"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("app-wrapper")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sidebar")))
                // marca com a logo na sidebar
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "images/hotelimpact-logo-simbolo.png")))
                .andReturn().getResponse().getContentAsString();

        // item de Reservas ativo e Dashboard não ativo (renderização server-side),
        // independente da ordem de atributos emitida pelo Thymeleaf
        var tagReservas = tagDoLink(resposta, "title=\"Reservas\"");
        org.junit.jupiter.api.Assertions.assertTrue(tagReservas.contains("class=\"active\""),
                "link de Reservas deve estar ativo: " + tagReservas);
        org.junit.jupiter.api.Assertions.assertTrue(tagReservas.contains("aria-current=\"page\""),
                "link de Reservas deve marcar aria-current: " + tagReservas);
        var tagDashboard = tagDoLink(resposta, "title=\"Dashboard\"");
        org.junit.jupiter.api.Assertions.assertFalse(tagDashboard.contains("class=\"active\""),
                "link de Dashboard não deve estar ativo em /reservas: " + tagDashboard);
    }

    private static String tagDoLink(String html, String marcador) {
        int i = html.indexOf(marcador);
        org.junit.jupiter.api.Assertions.assertTrue(i >= 0, "link não encontrado: " + marcador);
        int inicio = html.lastIndexOf("<a ", i);
        int fim = html.indexOf(">", i);
        return html.substring(inicio, fim);
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
