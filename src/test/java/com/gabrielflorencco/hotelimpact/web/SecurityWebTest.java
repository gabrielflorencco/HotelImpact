package com.gabrielflorencco.hotelimpact.web;

import com.gabrielflorencco.hotelimpact.controller.HomeController;
import com.gabrielflorencco.hotelimpact.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticação e CSRF (Fase 7): sem usuário, tudo redireciona ao login;
 * POST sem token CSRF é rejeitado mesmo autenticado; o login por formulário
 * com as credenciais configuradas autentica e devolve o usuário ao destino.
 */
@WebMvcTest(controllers = HomeController.class)
class SecurityWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void acessoSemAutenticacaoRedirecionaParaOLogin() throws Exception {
        mockMvc.perform(get("/").accept(org.springframework.http.MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void postAutenticadoSemCsrfEBloqueado() throws Exception {
        mockMvc.perform(post("/reservas").with(user("teste").roles("RECEPCAO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginComCredenciaisValidasAutenticaERedirecionaAoDestino() throws Exception {
        mockMvc.perform(formLogin("/login").user("teste").password("teste123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginComSenhaIncorretaPermaneceNaPaginaDeErroDeLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("teste").password("errada"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }
}
