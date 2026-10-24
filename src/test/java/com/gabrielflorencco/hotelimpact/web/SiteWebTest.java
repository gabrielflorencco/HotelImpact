package com.gabrielflorencco.hotelimpact.web;

import com.gabrielflorencco.hotelimpact.config.SecurityConfig;
import com.gabrielflorencco.hotelimpact.controller.SiteController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Site institucional público (MockMvc sem banco, com a SecurityConfig real
 * importada): as páginas públicas são acessíveis sem autenticação, o conteúdo
 * corresponde ao produto real e nada da área administrativa vaza nelas.
 */
@WebMvcTest(controllers = SiteController.class)
@Import(SecurityConfig.class)
class SiteWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void inicioPublicaRenderizaHeroPropostaEModulos() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("site/inicio"))
                .andExpect(content().string(containsString("Toda a operação do hotel")))
                .andExpect(content().string(containsString("Entrar no sistema")))
                .andExpect(content().string(containsString("Ilustração da fachada")))
                .andExpect(content().string(containsString("Nove módulos")));
    }

    @Test
    void sobrePublicoRenderizaContextoAcademicoSemFicticios() throws Exception {
        mockMvc.perform(get("/sobre").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("site/sobre"))
                .andExpect(content().string(containsString("trabalho acadêmico")))
                .andExpect(content().string(containsString("Não há")))
                .andExpect(content().string(not(containsString("depoimento de cliente"))));
    }

    @Test
    void funcionalidadesPublicaListaOsModulosReais() throws Exception {
        mockMvc.perform(get("/funcionalidades").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("site/funcionalidades"))
                .andExpect(content().string(containsString("Reservas")))
                .andExpect(content().string(containsString("Hospedagens")))
                .andExpect(content().string(containsString("Consumos e catálogo")))
                .andExpect(content().string(containsString("Pagamentos")))
                .andExpect(content().string(containsString("Manutenção")))
                .andExpect(content().string(containsString("Painel e relatórios")))
                .andExpect(content().string(containsString("Funcionalidades implementadas")));
    }

    @Test
    void siteNaoExpoeConteudoAdministrativoNemSegredos() throws Exception {
        String corpo = mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(corpo.contains("recepcao-dev"),
                "o site público não pode conter credenciais");
        org.junit.jupiter.api.Assertions.assertFalse(corpo.contains("Hospedagens em andamento"),
                "indicadores operacionais não pertencem ao site público");
    }
}
