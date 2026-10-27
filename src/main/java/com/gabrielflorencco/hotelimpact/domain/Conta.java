package com.gabrielflorencco.hotelimpact.domain;

import com.gabrielflorencco.hotelimpact.entity.Consumo;
import com.gabrielflorencco.hotelimpact.entity.Pagamento;

import java.math.BigDecimal;
import java.util.List;

/**
 * Conta de uma hospedagem: o fechamento da estadia como objeto de valor
 * imutável (record). Recebe os lançamentos já consultados — não toca no
 * banco — e concentra as fórmulas do negócio:
 *
 * <ul>
 *   <li>total = valor da reserva + consumos que contam (BR-PAG-003);</li>
 *   <li>saldo devedor = total − pagamentos que contam (BR-PAG-004);</li>
 *   <li>estornados não contam como recebidos; consumos cancelados saem.</li>
 * </ul>
 *
 * Os critérios de elegibilidade pertencem a cada lançamento (contrato
 * {@link Lancamento}) e a soma é feita pelo componente genérico
 * {@link Fechamento}.
 */
public record Conta(BigDecimal valorReserva, List<Consumo> consumos, List<Pagamento> pagamentos) {

    public Conta {
        consumos = List.copyOf(consumos);
        pagamentos = List.copyOf(pagamentos);
    }

    /** Total dos consumos elegíveis — parcela gerada durante a estadia. */
    public BigDecimal totalConsumos() {
        return Fechamento.totalElegivel(consumos);
    }

    /** Total pago: pagamentos confirmados (estornos saem). */
    public BigDecimal pago() {
        return Fechamento.totalElegivel(pagamentos);
    }

    /** Total da conta: reserva + consumos que contam. */
    public BigDecimal total() {
        return valorReserva.add(totalConsumos());
    }

    /** Saldo devedor: total − pago; negativo significa crédito. */
    public BigDecimal saldoDevedor() {
        return total().subtract(pago());
    }

    /** Conta quitada: saldo zero ou crédito a favor do hóspede. */
    public boolean quitada() {
        return saldoDevedor().signum() <= 0;
    }
}
