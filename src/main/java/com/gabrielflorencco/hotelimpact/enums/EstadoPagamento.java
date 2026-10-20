package com.gabrielflorencco.hotelimpact.enums;

/**
 * Estados do pagamento. Um pagamento CONFIRMADO é imutável quanto ao valor original:
 * correções usam estorno e novo lançamento. Estornados não contam como recebidos.
 */
public enum EstadoPagamento {
    CONFIRMADO,
    ESTORNADO
}
