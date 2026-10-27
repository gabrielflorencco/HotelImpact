package com.gabrielflorencco.hotelimpact.web;

import com.gabrielflorencco.hotelimpact.controller.HospedagemController;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import com.gabrielflorencco.hotelimpact.service.ConsumoService;
import com.gabrielflorencco.hotelimpact.service.HospedagemService;
import com.gabrielflorencco.hotelimpact.service.PagamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Fatia web das hospedagens (MockMvc sem banco): listagem, detalhe com a
 * conta (consumos e pagamentos com saldo), check-out — com justificativa de
 * encerramento excepcional quando há saldo pendente — e tratamento global
 * de erros.
 */
@WebMvcTest(controllers = HospedagemController.class)

@org.springframework.security.test.context.support.WithMockUser
class HospedagensWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HospedagemService hospedagemService;

    @MockitoBean
    private ConsumoService consumoService;

    @MockitoBean
    private PagamentoService pagamentoService;

    @MockitoBean
    private ItemCatalogoRepository itens;

    // ---- Listagem e detalhe ------------------------------------------------------------

    @Test
    void listarRenderizaTabelaComCodigoDaReserva() throws Exception {
        when(hospedagemService.buscar(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(hospedagemMock(7L, EstadoHospedagem.EM_ANDAMENTO))));

        mockMvc.perform(get("/hospedagens"))
                .andExpect(status().isOk())
                .andExpect(view().name("hospedagens/lista"))
                .andExpect(content().string(containsString("RES-2026-000123")))
                .andExpect(content().string(containsString("EM_ANDAMENTO")));
    }

    @Test
    void listarSemHospedagensExibeEstadoVazio() throws Exception {
        when(hospedagemService.buscar(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/hospedagens"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhuma hospedagem encontrada")));
    }

    @Test
    void detalheExibeSaldoPendenteEAvisoDeJustificativa() throws Exception {
        when(hospedagemService.buscarPorId(7L))
                .thenReturn(hospedagemMock(7L, EstadoHospedagem.EM_ANDAMENTO));
        stubConta(new BigDecimal("360.00"), BigDecimal.ZERO);

        mockMvc.perform(get("/hospedagens/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Realizar check-out")))
                .andExpect(content().string(containsString("saldo pendente")))
                .andExpect(content().string(containsString("Justificativa do encerramento excepcional")));
    }

    @Test
    void detalheDeHospedagemEncerradaNaoExibeBotaoDeCheckOut() throws Exception {
        Hospedagem encerrada = hospedagemMock(7L, EstadoHospedagem.ENCERRADA);
        encerrada.setCheckOutReal(LocalDateTime.now().plusHours(30));
        when(hospedagemService.buscarPorId(7L)).thenReturn(encerrada);
        stubConta(BigDecimal.ZERO, new BigDecimal("360.00"));

        mockMvc.perform(get("/hospedagens/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Realizar check-out"))));
    }

    @Test
    void detalheDeHospedagemInexistenteRetorna404ComPaginaDeErro() throws Exception {
        when(hospedagemService.buscarPorId(anyLong()))
                .thenThrow(new RegistroNaoEncontradoException("Hospedagem não encontrada."));

        mockMvc.perform(get("/hospedagens/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/erro-generico"))
                .andExpect(content().string(containsString("Hospedagem não encontrada")));
    }

    // ---- Check-out ----------------------------------------------------------------------

    @Test
    void checkOutSemJustificativaChamaServicoComMotivoNulo() throws Exception {
        mockMvc.perform(post("/hospedagens/7/check-out").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/hospedagens/7"));

        verify(hospedagemService).checkOut(7L, null);
    }

    @Test
    void checkOutComJustificativaEncaminhaOMotivo() throws Exception {
        mockMvc.perform(post("/hospedagens/7/check-out").with(csrf())
                        .param("motivoExcecao", "Hóspede se evadiu sem pagar."))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/hospedagens/7"));

        verify(hospedagemService).checkOut(7L, "Hóspede se evadiu sem pagar.");
    }

    @Test
    void checkOutBloqueadoPorRegraRedirecionaComErro() throws Exception {
        org.mockito.Mockito.doThrow(new NegocioException("A conta possui saldo pendente de R$ 360.00. "
                        + "Informe a justificativa do encerramento excepcional."))
                .when(hospedagemService).checkOut(org.mockito.ArgumentMatchers.eq(7L), isNull());

        mockMvc.perform(post("/hospedagens/7/check-out").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/hospedagens/7"));

        verify(hospedagemService).checkOut(7L, null);
    }

    // ---- Consumos da conta ---------------------------------------------------------------

    @Test
    void lancarConsumoValidoRedirecionaParaODetalhe() throws Exception {
        mockMvc.perform(post("/hospedagens/7/consumos").with(csrf())
                        .param("itemId", "1")
                        .param("quantidade", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/hospedagens/7"));

        verify(consumoService).lancar(org.mockito.ArgumentMatchers.eq(7L), any());
    }

    @Test
    void lancarConsumoInvalidoReRenderizaComErroDeCampo() throws Exception {
        when(hospedagemService.buscarPorId(7L))
                .thenReturn(hospedagemMock(7L, EstadoHospedagem.EM_ANDAMENTO));
        stubConta(new BigDecimal("360.00"), BigDecimal.ZERO);

        mockMvc.perform(post("/hospedagens/7/consumos").with(csrf()).param("itemId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("hospedagens/detalhe"))
                .andExpect(content().string(containsString("Campo obrigatório")));
    }

    @Test
    void lancarConsumoComRegraVioladaReRenderizaComMensagem() throws Exception {
        when(hospedagemService.buscarPorId(7L))
                .thenReturn(hospedagemMock(7L, EstadoHospedagem.EM_ANDAMENTO));
        stubConta(new BigDecimal("360.00"), BigDecimal.ZERO);
        when(consumoService.lancar(org.mockito.ArgumentMatchers.eq(7L), any()))
                .thenThrow(new NegocioException("Estoque insuficiente de \"Água mineral 500ml\": restam 1 unidade(s)."));

        mockMvc.perform(post("/hospedagens/7/consumos").with(csrf())
                        .param("itemId", "1")
                        .param("quantidade", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Estoque insuficiente")));
    }

    @Test
    void cancelarConsumoRedirecionaDeVoltaParaODetalhe() throws Exception {
        mockMvc.perform(post("/hospedagens/7/consumos/11/cancelar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/hospedagens/7"));

        verify(consumoService).cancelar(7L, 11L);
    }

    // ---- Pagamentos da conta --------------------------------------------------------------

    @Test
    void registrarPagamentoValidoRedirecionaParaODetalhe() throws Exception {
        mockMvc.perform(post("/hospedagens/7/pagamentos").with(csrf())
                        .param("valor", "250.00")
                        .param("forma", "PIX"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/hospedagens/7"));

        verify(pagamentoService).registrar(org.mockito.ArgumentMatchers.eq(7L), any());
    }

    @Test
    void registrarPagamentoInvalidoReRenderizaComErroDeCampo() throws Exception {
        when(hospedagemService.buscarPorId(7L))
                .thenReturn(hospedagemMock(7L, EstadoHospedagem.EM_ANDAMENTO));
        stubConta(new BigDecimal("360.00"), BigDecimal.ZERO);

        mockMvc.perform(post("/hospedagens/7/pagamentos").with(csrf()).param("forma", "PIX"))
                .andExpect(status().isOk())
                .andExpect(view().name("hospedagens/detalhe"))
                .andExpect(content().string(containsString("Campo obrigatório")));
    }

    @Test
    void registrarPagamentoEmHospedagemCanceladaReRenderizaComMensagem() throws Exception {
        when(hospedagemService.buscarPorId(7L))
                .thenReturn(hospedagemMock(7L, EstadoHospedagem.CANCELADA));
        stubConta(new BigDecimal("360.00"), BigDecimal.ZERO);
        when(pagamentoService.registrar(org.mockito.ArgumentMatchers.eq(7L), any()))
                .thenThrow(new NegocioException(
                        "A hospedagem foi encerrada excepcionalmente e não aceita novos pagamentos."));

        mockMvc.perform(post("/hospedagens/7/pagamentos").with(csrf())
                        .param("valor", "50.00")
                        .param("forma", "DINHEIRO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("não aceita novos pagamentos")));
    }

    @Test
    void estornarPagamentoRedirecionaDeVoltaParaODetalhe() throws Exception {
        mockMvc.perform(post("/hospedagens/7/pagamentos/3/estornar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/hospedagens/7"));

        verify(pagamentoService).estornar(7L, 3L);
    }

    // ---- Apoio -----------------------------------------------------------------------------

    /** Stubs da conta exigidos pelo template: consumos, total e saldo. */
    private void stubConta(BigDecimal saldo, BigDecimal totalPago) {
        // O detalhe fecha a conta pelo objeto de valor do domínio: usa uma
        // Conta REAL (reserva 360.00, sem consumos) com um pagamento
        // confirmado — saldo e pago derivam das fórmulas verdadeiras.
        when(consumoService.totalAtivos(7L)).thenReturn(BigDecimal.ZERO);
        java.util.List<com.gabrielflorencco.hotelimpact.entity.Pagamento> pagamentos =
                totalPago.signum() == 0 ? java.util.List.of()
                        : java.util.List.of(pagamentoConfirmado(totalPago));
        when(pagamentoService.contaDe(7L)).thenReturn(new com.gabrielflorencco.hotelimpact.domain.Conta(
                new BigDecimal("360.00"), java.util.List.of(), pagamentos));
    }

    private com.gabrielflorencco.hotelimpact.entity.Pagamento pagamentoConfirmado(BigDecimal valor) {
        com.gabrielflorencco.hotelimpact.entity.Pagamento p = new com.gabrielflorencco.hotelimpact.entity.Pagamento();
        p.setValor(valor);
        p.setEstado(com.gabrielflorencco.hotelimpact.enums.EstadoPagamento.CONFIRMADO);
        return p;
    }

    private Hospedagem hospedagemMock(Long id, EstadoHospedagem estado) {
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
        reserva.setDataCheckIn(LocalDate.now());
        reserva.setDataCheckOut(LocalDate.now().plusDays(2));
        reserva.setQuantidadeHospedes(2);
        reserva.setValorDiaria(new BigDecimal("180.00"));
        reserva.setDesconto(BigDecimal.ZERO);
        reserva.setValorTotal(new BigDecimal("360.00"));
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        Hospedagem hospedagem = new Hospedagem();
        hospedagem.setId(id);
        hospedagem.setReserva(reserva);
        hospedagem.setQuarto(quarto);
        hospedagem.setCheckInReal(LocalDateTime.now().minusHours(2));
        hospedagem.setEstado(estado);
        return hospedagem;
    }
}
