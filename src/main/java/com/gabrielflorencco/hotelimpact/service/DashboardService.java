package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import com.gabrielflorencco.hotelimpact.repository.PagamentoRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Indicadores do painel (Fase 6), calculados com consultas de contagem e
 * soma no banco — nada de carregar entidades para contar no aplicativo.
 * O dia de "hoje" é resolvido uma única vez para que todos os indicadores
 * refiram-se ao mesmo dia.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<EstadoReserva> RESERVAS_ATIVAS =
            List.of(EstadoReserva.PENDENTE, EstadoReserva.CONFIRMADA);

    private final QuartoRepository quartos;
    private final HospedagemRepository hospedagens;
    private final ReservaRepository reservas;
    private final PagamentoRepository pagamentos;
    private final ItemCatalogoRepository itens;

    /** Fotografia dos indicadores operacionais do hotel num único instante. */
    public record Indicadores(
            long quartosAtivos,
            long quartosDisponiveis,
            long quartosOcupados,
            long quartosEmLimpeza,
            long quartosEmManutencao,
            long quartosInterditados,
            long hospedagensEmAndamento,
            long chegadasPrevistasHoje,
            long saidasPrevistasHoje,
            BigDecimal receitaConfirmadaHoje,
            long itensComEstoqueBaixo) {
    }

    @Transactional(readOnly = true)
    public Indicadores indicadores() {
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioDoDia = hoje.atStartOfDay();
        LocalDateTime inicioDoDiaSeguinte = hoje.plusDays(1).atStartOfDay();

        return new Indicadores(
                quartos.countByAtivoTrue(),
                quartos.countByEstadoAndAtivoTrue(EstadoQuarto.DISPONIVEL),
                quartos.countByEstadoAndAtivoTrue(EstadoQuarto.OCUPADO),
                quartos.countByEstadoAndAtivoTrue(EstadoQuarto.LIMPEZA),
                quartos.countByEstadoAndAtivoTrue(EstadoQuarto.MANUTENCAO),
                quartos.countByEstadoAndAtivoTrue(EstadoQuarto.INTERDITADO),
                hospedagens.countByEstado(EstadoHospedagem.EM_ANDAMENTO),
                reservas.countByEstadoInAndDataCheckInEquals(RESERVAS_ATIVAS, hoje),
                reservas.countByEstadoInAndDataCheckOutEquals(RESERVAS_ATIVAS, hoje),
                pagamentos.somarPorEstadoEntre(EstadoPagamento.CONFIRMADO, inicioDoDia, inicioDoDiaSeguinte),
                itens.countComEstoqueBaixo());
    }

    /** Reservas ativas que chegam hoje, com hóspede e quarto já buscados (anti-N+1). */
    @Transactional(readOnly = true)
    public List<Reserva> chegadasDeHoje() {
        return reservas.buscarAtivasPorDataCheckInComDetalhes(RESERVAS_ATIVAS, LocalDate.now());
    }

    /** Reservas ativas com saída prevista para hoje, com hóspede e quarto já buscados. */
    @Transactional(readOnly = true)
    public List<Reserva> saidasDeHoje() {
        return reservas.buscarAtivasPorDataCheckOutComDetalhes(RESERVAS_ATIVAS, LocalDate.now());
    }
}
