package com.gabrielflorencco.hotelimpact.domain;

import java.math.BigDecimal;

/**
 * Lançamento da conta de uma hospedagem: tudo o que entra na conta de uma
 * estadia e que pode — ou não — contar no fechamento.
 *
 * Contrato de negócio com duas implementações reais e com regras distintas
 * (despachadas dinamicamente no fechamento da conta):
 * o {@code Consumo} conta quando está ATIVO (cancelado sai da conta) e o
 * {@code Pagamento} conta quando está CONFIRMADO (estornado não é recebido).
 */
public interface Lancamento {

    /** Valor monetário do lançamento. */
    BigDecimal valorContabil();

    /** Elegibilidade para o fechamento — regra própria de cada tipo real. */
    boolean contaNoFechamento();
}
