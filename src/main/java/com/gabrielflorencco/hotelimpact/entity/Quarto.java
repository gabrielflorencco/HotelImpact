package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
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
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Quarto do hotel. O estado operacional reflete a situação física atual do quarto;
 * a disponibilidade para um período de datas é calculada combinando este estado
 * com reservas e hospedagens conflitantes.
 */
@Entity
@Table(name = "quarto", indexes = {
        @Index(name = "idx_quarto_estado", columnList = "estado"),
        @Index(name = "idx_quarto_categoria", columnList = "categoria_id")
})
@Getter
@Setter
public class Quarto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, unique = true, length = 10)
    private String numero;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaQuarto categoria;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer andar;

    /** Capacidade operacional opcional; quando ausente, usa-se a capacidade da categoria. */
    @Min(1)
    @Column
    private Integer capacidadeOperacional;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoQuarto estado = EstadoQuarto.DISPONIVEL;

    @Size(max = 500)
    @Column(length = 500)
    private String observacoes;

    @NotNull
    @Column(nullable = false)
    private Boolean ativo = true;

    /** Versionamento otimista: reservas e check-in simultâneios disputam o mesmo quarto. */
    @Version
    private Long versao;

    /**
     * Capacidade usada nas validações de reserva: sobrescreve a da categoria
     * quando definida. Deve ser chamado dentro de uma transação.
     */
    public int getCapacidadeEfetiva() {
        return capacidadeOperacional != null ? capacidadeOperacional : categoria.getCapacidadeMaxima();
    }

    public boolean isOcupavel() {
        return estado == EstadoQuarto.DISPONIVEL;
    }
}
