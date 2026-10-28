package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import com.gabrielflorencco.hotelimpact.repository.PagamentoRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Indicadores do painel (unitário, sem Spring): cada campo do registro sai
 * da consulta correta, e o caso sem dados devolve zeros — receita zero,
 * nunca nula.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private QuartoRepository quartos;

    @Mock
    private HospedagemRepository hospedagens;

    @Mock
    private ReservaRepository reservas;

    @Mock
    private PagamentoRepository pagamentos;

    @Mock
    private ItemCatalogoRepository itens;

    @Mock
    private com.gabrielflorencco.hotelimpact.repository.TarefaLimpezaRepository tarefas;

    @Mock
    private com.gabrielflorencco.hotelimpact.service.AuditoriaService auditoria;

    @InjectMocks
    private DashboardService service;

    @BeforeEach
    void stubTarefas() {
        org.mockito.BDDMockito.given(tarefas.countByEstado(any())).willReturn(0L);
    }

    @Test
    void semDadosDevolveZerosComReceitaZeroNuncaNula() {
        when(quartos.countByAtivoTrue()).thenReturn(0L);
        when(quartos.countByEstadoAndAtivoTrue(any(EstadoQuarto.class))).thenReturn(0L);
        when(hospedagens.countByEstado(EstadoHospedagem.EM_ANDAMENTO)).thenReturn(0L);
        when(reservas.countByEstadoInAndDataCheckInEquals(anyList(), any(LocalDate.class))).thenReturn(0L);
        when(reservas.countByEstadoInAndDataCheckOutEquals(anyList(), any(LocalDate.class))).thenReturn(0L);
        when(pagamentos.somarPorEstadoEntre(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(itens.countComEstoqueBaixo()).thenReturn(0L);

        var ind = service.indicadores();

        assertThat(ind.quartosAtivos()).isZero();
        assertThat(ind.quartosDisponiveis()).isZero();
        assertThat(ind.quartosOcupados()).isZero();
        assertThat(ind.quartosEmLimpeza()).isZero();
        assertThat(ind.quartosEmManutencao()).isZero();
        assertThat(ind.quartosInterditados()).isZero();
        assertThat(ind.hospedagensEmAndamento()).isZero();
        assertThat(ind.chegadasPrevistasHoje()).isZero();
        assertThat(ind.saidasPrevistasHoje()).isZero();
        assertThat(ind.receitaConfirmadaHoje()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ind.itensComEstoqueBaixo()).isZero();
    }

    @Test
    void cadaIndicadorSaiDaConsultaCorretaDoDiaAtual() {
        when(quartos.countByAtivoTrue()).thenReturn(12L);
        when(quartos.countByEstadoAndAtivoTrue(EstadoQuarto.DISPONIVEL)).thenReturn(4L);
        when(quartos.countByEstadoAndAtivoTrue(EstadoQuarto.OCUPADO)).thenReturn(5L);
        when(quartos.countByEstadoAndAtivoTrue(EstadoQuarto.LIMPEZA)).thenReturn(1L);
        when(quartos.countByEstadoAndAtivoTrue(EstadoQuarto.MANUTENCAO)).thenReturn(1L);
        when(quartos.countByEstadoAndAtivoTrue(EstadoQuarto.INTERDITADO)).thenReturn(1L);
        when(hospedagens.countByEstado(EstadoHospedagem.EM_ANDAMENTO)).thenReturn(5L);
        when(reservas.countByEstadoInAndDataCheckInEquals(anyList(), any(LocalDate.class))).thenReturn(3L);
        when(reservas.countByEstadoInAndDataCheckOutEquals(anyList(), any(LocalDate.class))).thenReturn(2L);
        when(pagamentos.somarPorEstadoEntre(any(), any(), any())).thenReturn(new BigDecimal("1234.50"));
        when(itens.countComEstoqueBaixo()).thenReturn(2L);

        var ind = service.indicadores();

        assertThat(ind.quartosAtivos()).isEqualTo(12);
        assertThat(ind.quartosDisponiveis()).isEqualTo(4);
        assertThat(ind.quartosOcupados()).isEqualTo(5);
        assertThat(ind.quartosEmLimpeza()).isEqualTo(1);
        assertThat(ind.quartosEmManutencao()).isEqualTo(1);
        assertThat(ind.quartosInterditados()).isEqualTo(1);
        assertThat(ind.hospedagensEmAndamento()).isEqualTo(5);
        assertThat(ind.chegadasPrevistasHoje()).isEqualTo(3);
        assertThat(ind.saidasPrevistasHoje()).isEqualTo(2);
        assertThat(ind.receitaConfirmadaHoje()).isEqualByComparingTo("1234.50");
        assertThat(ind.itensComEstoqueBaixo()).isEqualTo(2);

        // Contagens de reservas consideram apenas estados ativos; receita soma só o dia atual.
        verify(reservas).countByEstadoInAndDataCheckInEquals(
                eq(List.of(EstadoReserva.PENDENTE, EstadoReserva.CONFIRMADA)), eq(LocalDate.now()));
        LocalDate hoje = LocalDate.now();
        verify(pagamentos).somarPorEstadoEntre(eq(EstadoPagamento.CONFIRMADO),
                eq(hoje.atStartOfDay()), eq(hoje.plusDays(1).atStartOfDay()));
    }

    @Test
    void somaDeReceitaUsaIntervaloSemiabertoDoDia() {
        when(pagamentos.somarPorEstadoEntre(any(), any(), any())).thenReturn(BigDecimal.TEN);
        when(quartos.countByAtivoTrue()).thenReturn(0L);
        when(hospedagens.countByEstado(EstadoHospedagem.EM_ANDAMENTO)).thenReturn(0L);
        when(itens.countComEstoqueBaixo()).thenReturn(0L);

        service.indicadores();

        LocalDateTime inicioEsperado = LocalDate.now().atStartOfDay();
        verify(pagamentos).somarPorEstadoEntre(any(), eq(inicioEsperado), eq(inicioEsperado.plusDays(1)));
    }
}
