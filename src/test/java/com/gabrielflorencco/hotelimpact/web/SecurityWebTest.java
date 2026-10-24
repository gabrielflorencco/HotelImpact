package com.gabrielflorencco.hotelimpact.web;

import com.gabrielflorencco.hotelimpact.config.SecurityConfig;
import com.gabrielflorencco.hotelimpact.controller.HomeController;
import com.gabrielflorencco.hotelimpact.controller.LoginController;
import com.gabrielflorencco.hotelimpact.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticação e CSRF contra a SecurityConfig real (importada no slice):
 * sem usuário, tudo redireciona ao login próprio; POST sem token CSRF é
 * rejeitado mesmo autenticado; o login por formulário funciona com as
 * credenciais configuradas; o logout encerra a sessão e volta ao login.
 */
@WebMvcTest(controllers = {HomeController.class, LoginController.class})
@Import(SecurityConfig.class)
class SecurityWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void acessoSemAutenticacaoRedirecionaParaOLogin() throws Exception {
        mockMvc.perform(get("/painel").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void rotaAdministrativaProtegidaRedirecionaAoLogin() throws Exception {
        mockMvc.perform(get("/reservas").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void paginaDeLoginPropriaRenderizaEmPortuguesSemExigirAutenticacao() throws Exception {
        mockMvc.perform(get("/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("HotelImpact")))
                .andExpect(content().string(containsString("Gestão hoteleira")))
                .andExpect(content().string(containsString("Usuário")))
                .andExpect(content().string(containsString("Entrar")));
    }

    @Test
    void loginInvalidoRedirecionaComErroEOLogoutComMensagemDeSaida() throws Exception {
        mockMvc.perform(get("/login").param("error", "").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuário ou senha inválidos")));

        mockMvc.perform(get("/login").param("logout", "").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sessão encerrada")));
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
                .andExpect(redirectedUrl("/painel"));
    }

    @Test
    void loginComSenhaIncorretaPermaneceNaPaginaDeErroDeLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("teste").password("errada"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void logoutEncerraSessaoERedirecionaAoLoginComMensagem() throws Exception {
        mockMvc.perform(post("/logout").with(user("teste").roles("RECEPCAO")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }
}
