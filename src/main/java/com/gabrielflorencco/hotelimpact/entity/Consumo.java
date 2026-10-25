package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.enums.EstadoConsumo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lançamento de consumo em uma hospedagem. O preço unitário é capturado no
 * lançamento para preservar o histórico. Lançamentos nunca são excluídos:
 * correções usam cancelamento (estorno lógico) que restitui o estoque uma única vez.
 */
@Entity
@Table(name = "consumo", indexes = {
        @Index(name = "idx_consumo_hospedagem", columnList = "hospedagem_id")
})
@Getter
@Setter
public class Consumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Hospedagem hospedagem;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemCatalogo item;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantidade;

    /** Preço unitário histórico no momento do lançamento. */
    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    /** Total calculado no backend: quantidade × preço unitário. */
    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoConsumo estado = EstadoConsumo.ATIVO;

    @Size(max = 300)
    @Column(length = 300)
    private String observacoes;
}
