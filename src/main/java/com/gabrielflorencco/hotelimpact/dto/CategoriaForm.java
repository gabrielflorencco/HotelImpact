package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Formulário de categoria de quarto. Os valores enviados pelo navegador
 * nunca são vinculados diretamente à entidade JPA.
 */
@Getter
@Setter
public class CategoriaForm {

    private Long id;

    @NotBlank
    @Size(min = 3, max = 80)
    private String nome;

    @Size(max = 500)
    private String descricao;

    @NotNull
    @Min(1)
    private Integer capacidadeMaxima;

    @NotNull
    @Min(1)
    private Integer quantidadeCamas;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal precoBase;

    private Boolean ativo = true;

    public static CategoriaForm da(CategoriaQuarto categoria) {
        CategoriaForm form = new CategoriaForm();
        form.setId(categoria.getId());
        form.setNome(categoria.getNome());
        form.setDescricao(categoria.getDescricao());
        form.setCapacidadeMaxima(categoria.getCapacidadeMaxima());
        form.setQuantidadeCamas(categoria.getQuantidadeCamas());
        form.setPrecoBase(categoria.getPrecoBase());
        form.setAtivo(categoria.getAtivo());
        return form;
    }
}
