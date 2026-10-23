package com.gabrielflorencco.hotelimpact.web;

import com.gabrielflorencco.hotelimpact.controller.ItemCatalogoController;
import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.service.ItemCatalogoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Fatia web do catálogo de itens (MockMvc sem banco): listagem com badge de
 * estoque baixo, validação de formulário, nome duplicado e PRG.
 */
@WebMvcTest(controllers = ItemCatalogoController.class)
class ItensWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemCatalogoService itemService;

    @Test
    void listarRenderizaNomeTipoEAlertaDeEstoqueBaixo() throws Exception {
        ItemCatalogo agua = new ItemCatalogo();
        agua.setId(1L);
        agua.setNome("Água mineral 500ml");
        agua.setTipo(TipoItem.PRODUTO);
        agua.setPrecoUnitario(new BigDecimal("5.00"));
        agua.setControlaEstoque(true);
        agua.setEstoqueAtual(2);
        agua.setEstoqueMinimo(20);
        agua.setAtivo(true);
        when(itemService.listar()).thenReturn(List.of(agua));

        mockMvc.perform(get("/itens"))
                .andExpect(status().isOk())
                .andExpect(view().name("itens/lista"))
                .andExpect(content().string(containsString("Água mineral 500ml")))
                .andExpect(content().string(containsString("estoque baixo")));
    }

    @Test
    void formularioNovoRenderizaTiposDisponiveis() throws Exception {
        mockMvc.perform(get("/itens/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("itens/formulario"))
                .andExpect(content().string(containsString("PRODUTO")))
                .andExpect(content().string(containsString("SERVICO")));
    }

    @Test
    void criarComFormularioInvalidoReRenderizaSemChamarOService() throws Exception {
        mockMvc.perform(post("/itens").param("nome", "Só o nome"))
                .andExpect(status().isOk())
                .andExpect(view().name("itens/formulario"))
                .andExpect(content().string(containsString("Campo obrigatório")));

        verify(itemService, never()).salvar(any());
    }

    @Test
    void criarValidoRedirecionaParaAListagem() throws Exception {
        mockMvc.perform(post("/itens")
                        .param("nome", "Água mineral 500ml")
                        .param("tipo", "PRODUTO")
                        .param("precoUnitario", "5.00")
                        .param("controlaEstoque", "true")
                        .param("estoqueAtual", "100")
                        .param("estoqueMinimo", "20")
                        .param("ativo", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/itens"));

        verify(itemService).salvar(any());
    }

    @Test
    void criarComNomeDuplicadoReRenderizaComMensagemDeErro() throws Exception {
        when(itemService.salvar(any()))
                .thenThrow(new NegocioException("Já existe um item com o nome informado."));

        mockMvc.perform(post("/itens")
                        .param("nome", "Água mineral 500ml")
                        .param("tipo", "PRODUTO")
                        .param("precoUnitario", "5.00")
                        .param("ativo", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("itens/formulario"))
                .andExpect(content().string(containsString("Já existe um item")));
    }

    @Test
    void excluirComHistoricoRedirecionaComErro() throws Exception {
        org.mockito.Mockito.doThrow(new NegocioException(
                        "Não é possível excluir o item: existem consumos registrados que preservam o histórico. Desative-o."))
                .when(itemService).excluir(1L);

        mockMvc.perform(post("/itens/1/excluir"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/itens"));

        verify(itemService).excluir(1L);
    }
}
