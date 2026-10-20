package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import com.gabrielflorencco.hotelimpact.enums.FormaPagamento;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pagamento vinculado a uma hospedagem (múltiplos pagamentos são suportados).
 * Um pagamento confirmado é imutável quanto ao valor original; correções usam
 * estorno (uma única vez) e novo lançamento. Não armazena dados de cartão.
 */
@Entity
@Table(name = "pagamento", indexes = {
        @Index(name = "idx_pagamento_hospedagem", columnList = "hospedagem_id"),
        @Index(name = "idx_pagamento_estado_data", columnList = "estado, data_hora")
})
@Getter
@Setter
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Hospedagem hospedagem;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaPagamento forma;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoPagamento estado = EstadoPagamento.CONFIRMADO;

    @Size(max = 300)
    @Column(length = 300)
    private String observacao;
}
