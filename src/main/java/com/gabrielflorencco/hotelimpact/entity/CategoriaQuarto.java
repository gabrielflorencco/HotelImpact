package com.gabrielflorencco.hotelimpact.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
 * Categoria de acomodação (ex.: Standard, Luxo, Suíte). Define capacidade de referência
 * e preço-base da diária; o preço acordado em cada reserva é sempre histórico.
 */
@Entity
@Table(name = "categoria_quarto")
@Getter
@Setter
public class CategoriaQuarto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 80)
    @Column(nullable = false, unique = true, length = 80)
    private String nome;

    @Size(max = 500)
    @Column(length = 500)
    private String descricao;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer capacidadeMaxima;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantidadeCamas;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoBase;

    @NotNull
    @Column(nullable = false)
    private Boolean ativo = true;
}
