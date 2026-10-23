package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.enums.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Formulário de registro de pagamento numa hospedagem. O pagamento nasce
 * CONFIRMADO; correções usam estorno e novo lançamento — nunca edição.
 */
@Getter
@Setter
public class PagamentoForm {

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal valor;

    @NotNull
    private FormaPagamento forma;

    @Size(max = 300)
    private String observacao;
}
