package com.gabrielflorencco.hotelimpact.enums;

/**
 * Estados do lançamento de consumo. CANCELADO representa o estorno lógico:
 * o lançamento é preservado e o estoque é restituído uma única vez.
 */
public enum EstadoConsumo {
    ATIVO,
    CANCELADO
}
