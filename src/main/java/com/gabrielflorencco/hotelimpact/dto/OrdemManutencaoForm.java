package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.enums.PrioridadeManutencao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Formulário de abertura de ordem de manutenção. O estado e o flag de
 * interdição são definidos pelo serviço conforme a prioridade e a situação
 * atual do quarto; ordens não são editadas — correções usam cancelamento.
 */
@Getter
@Setter
public class OrdemManutencaoForm {

    @NotNull
    private Long quartoId;

    @NotBlank
    @Size(min = 3, max = 120)
    private String titulo;

    @Size(max = 500)
    private String descricao;

    @NotNull
    private PrioridadeManutencao prioridade;

    @Size(max = 300)
    private String observacoes;
}
