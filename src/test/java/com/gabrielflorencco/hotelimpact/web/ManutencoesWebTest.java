package com.gabrielflorencco.hotelimpact.web;

import com.gabrielflorencco.hotelimpact.controller.ManutencaoController;
import com.gabrielflorencco.hotelimpact.entity.OrdemManutencao;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoManutencao;
import com.gabrielflorencco.hotelimpact.enums.PrioridadeManutencao;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.service.ManutencaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
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
 * Fatia web das ordens de manutenção (MockMvc sem banco): listagem, detalhe
 * com ações conforme o estado, validação de formulário, PRG e 404.
 */
@WebMvcTest(controllers = ManutencaoController.class)

@org.springframework.security.test.context.support.WithMockUser
class ManutencoesWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManutencaoService manutencaoService;

    @MockitoBean
    private QuartoRepository quartos;

    @Test
    void listarRenderizaTituloQuartoEPrioridade() throws Exception {
        when(manutencaoService.buscar(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ordemMock(5L, EstadoManutencao.ABERTA,
                        PrioridadeManutencao.CRITICA, true))));

        mockMvc.perform(get("/manutencoes"))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencoes/lista"))
                .andExpect(content().string(containsString("Vazamento no banheiro")))
                .andExpect(content().string(containsString("101")))
                .andExpect(content().string(containsString("interditou")));
    }

    @Test
    void listarVazioExibeEstadoVazio() throws Exception {
        when(manutencaoService.buscar(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/manutencoes"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhuma ordem de manutenção encontrada")));
    }

    @Test
    void formularioNovoRenderizaPrioridades() throws Exception {
        mockMvc.perform(get("/manutencoes/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencoes/formulario"))
                .andExpect(content().string(containsString("CRITICA")));
    }

    @Test
    void criarComFormularioInvalidoReRenderizaSemChamarOService() throws Exception {
        mockMvc.perform(post("/manutencoes").with(csrf()).param("titulo", "Só o título"))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencoes/formulario"))
                .andExpect(content().string(containsString("Campo obrigatório")));

        verify(manutencaoService, never()).abrir(any());
    }

    @Test
    void criarValidoRedirecionaParaODetalheDaOrdem() throws Exception {
        when(manutencaoService.abrir(any())).thenReturn(ordemMock(5L, EstadoManutencao.ABERTA,
                PrioridadeManutencao.MEDIA, false));

        mockMvc.perform(post("/manutencoes").with(csrf())
                        .param("quartoId", "1")
                        .param("titulo", "Troca de lâmpada")
                        .param("prioridade", "MEDIA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/manutencoes/5"));

        verify(manutencaoService).abrir(any());
    }

    @Test
    void criarComRegraVioladaReRenderizaComMensagem() throws Exception {
        when(manutencaoService.abrir(any()))
                .thenThrow(new NegocioException(
                        "Não é possível abrir a manutenção: existe uma hospedagem em andamento no quarto 101."));

        mockMvc.perform(post("/manutencoes").with(csrf())
                        .param("quartoId", "1")
                        .param("titulo", "Reparo com hóspede")
                        .param("prioridade", "BAIXA"))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencoes/formulario"))
                .andExpect(content().string(containsString("hospedagem em andamento")));
    }

    @Test
    void iniciarConcluirECancelarRedirecionamComPRG() throws Exception {
        mockMvc.perform(post("/manutencoes/5/iniciar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/manutencoes/5"));
        verify(manutencaoService).iniciar(5L);

        mockMvc.perform(post("/manutencoes/5/concluir").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/manutencoes/5"));
        verify(manutencaoService).concluir(5L);

        mockMvc.perform(post("/manutencoes/5/cancelar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/manutencoes/5"));
        verify(manutencaoService).cancelar(5L);
    }

    @Test
    void detalheDeOrdemInexistenteRetorna404ComPaginaDeErro() throws Exception {
        when(manutencaoService.buscarPorId(anyLong()))
                .thenThrow(new RegistroNaoEncontradoException("Ordem de manutenção não encontrada."));

        mockMvc.perform(get("/manutencoes/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/erro-generico"))
                .andExpect(content().string(containsString("Ordem de manutenção não encontrada")));
    }

    private OrdemManutencao ordemMock(Long id, EstadoManutencao estado,
                                      PrioridadeManutencao prioridade, boolean interditou) {
        Quarto quarto = new Quarto();
        quarto.setId(20L);
        quarto.setNumero("101");

        OrdemManutencao ordem = new OrdemManutencao();
        ordem.setId(id);
        ordem.setQuarto(quarto);
        ordem.setTitulo("Vazamento no banheiro");
        ordem.setDescricao("Descrição de teste.");
        ordem.setPrioridade(prioridade);
        ordem.setEstado(estado);
        ordem.setInterditouQuarto(interditou);
        ordem.setAbertaEm(LocalDateTime.now().minusHours(3));
        return ordem;
    }
}
