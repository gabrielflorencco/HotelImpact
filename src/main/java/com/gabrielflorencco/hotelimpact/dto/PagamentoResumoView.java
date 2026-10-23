package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.Pagamento;
import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import com.gabrielflorencco.hotelimpact.enums.FormaPagamento;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Pagamento exibido na conta da hospedagem. Estornados permanecem na lista
 * para preservar o histórico, mas não contam como recebidos.
 */
@Getter
public class PagamentoResumoView {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Long id;
    private final FormaPagamento forma;
    private final BigDecimal valor;
    private final String dataHora;
    private final EstadoPagamento estado;
    private final String observacao;

    private PagamentoResumoView(Pagamento pagamento) {
        this.id = pagamento.getId();
        this.forma = pagamento.getForma();
        this.valor = pagamento.getValor();
        this.dataHora = pagamento.getDataHora() == null ? null : pagamento.getDataHora().format(DATA_HORA);
        this.estado = pagamento.getEstado();
        this.observacao = pagamento.getObservacao();
    }

    public static PagamentoResumoView de(Pagamento pagamento) {
        return new PagamentoResumoView(pagamento);
    }
}
