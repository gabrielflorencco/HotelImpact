package com.gabrielflorencco.hotelimpact.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cálculo de fechamento de conta: soma apenas os lançamentos elegíveis.
 *
 * Componente genérico próprio do domínio: o parâmetro de tipo {@code T}
 * mantém a lista concretamente tipada ({@code List<Consumo>},
 * {@code List<Pagamento>}) e a elegibilidade de cada item é decidida pelo
 * próprio lançamento, via contrato {@link Lancamento} — sem casts e sem
 * duplicar a fórmula por tipo.
 */
public final class Fechamento {

    private Fechamento() {
    }

    /** Soma o valor contábil dos lançamentos elegíveis de qualquer tipo concreto. */
    public static <T extends Lancamento> BigDecimal totalElegivel(List<T> lancamentos) {
        return lancamentos.stream()
                .filter(Lancamento::contaNoFechamento)
                .map(Lancamento::valorContabil)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
