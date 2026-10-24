package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.PagamentoRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Relatório por período (unitário, sem Spring): cada número sai da consulta
 * correta com o intervalo semiaberto [inicio, fim), e o caso sem dados
 * devolve zeros com receita zero — nunca nula.
 */
@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private HospedagemRepository hospedagens;

    @Mock
    private ReservaRepository reservas;

    @Mock
    private PagamentoRepository pagamentos;

    @InjectMocks
    private RelatorioService service;

    @Test
    void semDadosNoPeriodoDevolveZerosComReceitaZero() {
        when(pagamentos.somarPorEstadoEntre(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(hospedagens.countByCheckInRealBetween(any(), any())).thenReturn(0L);
        when(hospedagens.countByCheckOutRealBetween(any(), any())).thenReturn(0L);
        when(hospedagens.buscarIniciadasEntre(any(), any())).thenReturn(List.of());
        when(reservas.countByDataCriacaoBetween(any(), any())).thenReturn(0L);
        when(reservas.countByEstadoAndDataCriacaoBetween(any(), any(), any())).thenReturn(0L);

        var resumo = service.resumoPeriodo(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(resumo.inicio()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(resumo.fim()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(resumo.receitaConfirmada()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumo.checkIns()).isZero();
        assertThat(resumo.checkOuts()).isZero();
        assertThat(resumo.reservasCriadas()).isZero();
        assertThat(resumo.reservasCanceladas()).isZero();
        assertThat(resumo.noShows()).isZero();
        assertThat(resumo.hospedagensIniciadas()).isEmpty();
    }

    @Test
    void cadaNumeroSaiDaConsultaCorretaComIntervaloSemiaberto() {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fim = LocalDate.of(2026, 10, 31);
        LocalDateTime inicioEsperado = inicio.atStartOfDay();
        LocalDateTime fimEsperado = fim.plusDays(1).atStartOfDay();

        when(pagamentos.somarPorEstadoEntre(any(), any(), any())).thenReturn(new BigDecimal("2500.00"));
        when(hospedagens.countByCheckInRealBetween(any(), any())).thenReturn(7L);
        when(hospedagens.countByCheckOutRealBetween(any(), any())).thenReturn(5L);
        when(hospedagens.buscarIniciadasEntre(any(), any())).thenReturn(List.of(new Hospedagem()));
        when(reservas.countByDataCriacaoBetween(any(), any())).thenReturn(12L);
        when(reservas.countByEstadoAndDataCriacaoBetween(eq(EstadoReserva.CANCELADA), any(), any())).thenReturn(2L);
        when(reservas.countByEstadoAndDataCriacaoBetween(eq(EstadoReserva.NO_SHOW), any(), any())).thenReturn(1L);

        var resumo = service.resumoPeriodo(inicio, fim);

        assertThat(resumo.receitaConfirmada()).isEqualByComparingTo("2500.00");
        assertThat(resumo.checkIns()).isEqualTo(7);
        assertThat(resumo.checkOuts()).isEqualTo(5);
        assertThat(resumo.reservasCriadas()).isEqualTo(12);
        assertThat(resumo.reservasCanceladas()).isEqualTo(2);
        assertThat(resumo.noShows()).isEqualTo(1);
        assertThat(resumo.hospedagensIniciadas()).hasSize(1);

        verify(pagamentos).somarPorEstadoEntre(eq(EstadoPagamento.CONFIRMADO),
                eq(inicioEsperado), eq(fimEsperado));
        verify(hospedagens).buscarIniciadasEntre(eq(inicioEsperado), eq(fimEsperado));
    }
}
