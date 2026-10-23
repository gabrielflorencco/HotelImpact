package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Formulário de item do catálogo. Serviços nunca controlam estoque — o
 * service ignora a flag nesses casos, preservando a invariante da entidade.
 */
@Getter
@Setter
public class ItemCatalogoForm {

    private Long id;

    @NotBlank
    @Size(min = 2, max = 80)
    private String nome;

    @Size(max = 300)
    private String descricao;

    @NotNull
    private TipoItem tipo;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal precoUnitario;

    /** Ignorado para serviços: eles nunca controlam estoque. */
    private Boolean controlaEstoque = false;

    @PositiveOrZero
    @Max(100_000)
    private Integer estoqueAtual = 0;

    @PositiveOrZero
    @Max(100_000)
    private Integer estoqueMinimo = 0;

    private Boolean ativo = true;

    public static ItemCatalogoForm de(ItemCatalogo item) {
        ItemCatalogoForm form = new ItemCatalogoForm();
        form.setId(item.getId());
        form.setNome(item.getNome());
        form.setDescricao(item.getDescricao());
        form.setTipo(item.getTipo());
        form.setPrecoUnitario(item.getPrecoUnitario());
        form.setControlaEstoque(item.getControlaEstoque());
        form.setEstoqueAtual(item.getEstoqueAtual());
        form.setEstoqueMinimo(item.getEstoqueMinimo());
        form.setAtivo(item.getAtivo());
        return form;
    }
}
