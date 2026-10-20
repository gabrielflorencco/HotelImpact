package com.gabrielflorencco.hotelimpact.enums;

/**
 * Estados da reserva. PENDENTE e CONFIRMADA bloqueiam a disponibilidade do quarto;
 * CANCELADA, CONCLUIDA e NO_SHOW não bloqueiam períodos futuros.
 */
public enum EstadoReserva {
    PENDENTE,
    CONFIRMADA,
    CANCELADA,
    CONCLUIDA,
    NO_SHOW
}
