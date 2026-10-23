package com.gabrielflorencco.hotelimpact.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Formulário de lançamento de consumo em uma hospedagem. O preço unitário é
 * sempre capturado do catálogo no momento do lançamento (valor histórico);
 * o total é calculado no servidor.
 */
@Getter
@Setter
public class ConsumoForm {

    @NotNull
    private Long itemId;

    @NotNull
    @Min(1)
    @Max(99)
    private Integer quantidade;

    @Size(max = 300)
    private String observacoes;
}
