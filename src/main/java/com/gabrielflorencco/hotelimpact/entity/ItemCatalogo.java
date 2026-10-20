package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Item do catálogo de produtos e serviços consumíveis. Produtos podem controlar
 * estoque; serviços nunca controlam. O estoque nunca pode ficar negativo
 * (também garantido por constraint de banco).
 */
@Entity
@Table(name = "item_catalogo")
@Getter
@Setter
public class ItemCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 80)
    @Column(nullable = false, unique = true, length = 80)
    private String nome;

    @Size(max = 300)
    @Column(length = 300)
    private String descricao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoItem tipo;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @NotNull
    @Column(name = "controla_estoque", nullable = false)
    private Boolean controlaEstoque = false;

    @NotNull
    @PositiveOrZero
    @Column(name = "estoque_atual", nullable = false)
    private Integer estoqueAtual = 0;

    @NotNull
    @PositiveOrZero
    @Column(name = "estoque_minimo", nullable = false)
    private Integer estoqueMinimo = 0;

    @NotNull
    @Column(nullable = false)
    private Boolean ativo = true;

    /** Versionamento otimista: consumos simultâneos disputam o mesmo estoque. */
    @Version
    private Long versao;

    public boolean isEstoqueBaixo() {
        return controlaEstoque && estoqueAtual <= estoqueMinimo;
    }

    /** Serviços nunca controlam estoque, independente da flag. */
    public boolean isEstoqueControladoEfetivo() {
        return tipo == TipoItem.PRODUTO && controlaEstoque;
    }
}
