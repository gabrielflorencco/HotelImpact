package com.gabrielflorencco.hotelimpact.enums;

/**
 * Estado operacional do quarto. Diferente da disponibilidade para um intervalo de datas,
 * que é calculada a partir das reservas e hospedagens existentes.
 */
public enum EstadoQuarto {
    DISPONIVEL,
    OCUPADO,
    LIMPEZA,
    MANUTENCAO,
    INTERDITADO
}
