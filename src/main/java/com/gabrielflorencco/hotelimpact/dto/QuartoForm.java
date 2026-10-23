package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.Quarto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Formulário de quarto. O estado operacional não é editável aqui:
 * ele muda apenas por operações de negócio explícitas (check-in, limpeza,
 * manutenção, interdição).
 */
@Getter
@Setter
public class QuartoForm {

    private Long id;

    @NotBlank
    @Size(max = 10)
    private String numero;

    @NotNull
    private Long categoriaId;

    @NotNull
    @PositiveOrZero
    @Max(200)
    private Integer andar;

    /** Capacidade operacional opcional; vazia usa a capacidade da categoria. */
    @Min(1)
    @Max(20)
    private Integer capacidadeOperacional;

    @Size(max = 500)
    private String observacoes;

    private Boolean ativo = true;

    public static QuartoForm de(Quarto quarto) {
        QuartoForm form = new QuartoForm();
        form.setId(quarto.getId());
        form.setNumero(quarto.getNumero());
        form.setCategoriaId(quarto.getCategoria().getId());
        form.setAndar(quarto.getAndar());
        form.setCapacidadeOperacional(quarto.getCapacidadeOperacional());
        form.setObservacoes(quarto.getObservacoes());
        form.setAtivo(quarto.getAtivo());
        return form;
    }
}
