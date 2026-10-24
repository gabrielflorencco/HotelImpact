package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.PagamentoRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Relatórios por período (Fase 6): números calculados com consultas de
 * contagem e soma no banco — check-ins/check-outs reais, receita confirmada,
 * reservas criadas (por estado, incluindo cancelamentos e no-shows) e a
 * lista das hospedagens iniciadas no período. Períodos são semiabertos
 * [inicio, fim), medidos à meia-noite.
 */
@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final HospedagemRepository hospedagens;
    private final ReservaRepository reservas;
    private final PagamentoRepository pagamentos;

    /** Resumo do período semiaberto [inicio, fim) com as hospedagens iniciadas nele. */
    public record ResumoPeriodo(
            LocalDate inicio,
            LocalDate fim,
            BigDecimal receitaConfirmada,
            long checkIns,
            long checkOuts,
            long reservasCriadas,
            long reservasCanceladas,
            long noShows,
            List<Hospedagem> hospedagensIniciadas) {
    }

    @Transactional(readOnly = true)
    public ResumoPeriodo resumoPeriodo(LocalDate inicio, LocalDate fim) {
        LocalDateTime inicioDoDia = inicio.atStartOfDay();
        LocalDateTime inicioDoDiaSeguinte = fim.plusDays(1).atStartOfDay();

        return new ResumoPeriodo(
                inicio,
                fim,
                pagamentos.somarPorEstadoEntre(EstadoPagamento.CONFIRMADO, inicioDoDia, inicioDoDiaSeguinte),
                hospedagens.countByCheckInRealBetween(inicioDoDia, inicioDoDiaSeguinte),
                hospedagens.countByCheckOutRealBetween(inicioDoDia, inicioDoDiaSeguinte),
                reservas.countByDataCriacaoBetween(inicioDoDia, inicioDoDiaSeguinte),
                reservas.countByEstadoAndDataCriacaoBetween(EstadoReserva.CANCELADA, inicioDoDia, inicioDoDiaSeguinte),
                reservas.countByEstadoAndDataCriacaoBetween(EstadoReserva.NO_SHOW, inicioDoDia, inicioDoDiaSeguinte),
                hospedagens.buscarIniciadasEntre(inicioDoDia, inicioDoDiaSeguinte));
    }
}
