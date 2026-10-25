package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.enums.EstadoManutencao;
import com.gabrielflorencco.hotelimpact.enums.PrioridadeManutencao;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ordem de manutenção de um quarto. Ordens com interdição (ex.: CRÍTICA) tiram
 * o quarto de operação; a abertura valida a consistência com hospedagens ativas.
 */
@Entity
@Table(name = "ordem_manutencao", indexes = {
        @Index(name = "idx_ordem_manutencao_quarto", columnList = "quarto_id"),
        @Index(name = "idx_ordem_manutencao_estado", columnList = "estado")
})
@Getter
@Setter
public class OrdemManutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Quarto quarto;

    @NotBlank
    @Size(min = 3, max = 120)
    @Column(nullable = false, length = 120)
    private String titulo;

    @Size(max = 500)
    @Column(length = 500)
    private String descricao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrioridadeManutencao prioridade = PrioridadeManutencao.MEDIA;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoManutencao estado = EstadoManutencao.ABERTA;

    @Column(nullable = false, updatable = false)
    private LocalDateTime abertaEm;

    @Column
    private LocalDateTime concluidaEm;

    /** Indica se esta ordem interditou o quarto (usado no cálculo da ocupação). */
    @NotNull
    @Column(nullable = false)
    private Boolean interditouQuarto = false;

    @Size(max = 300)
    @Column(length = 300)
    private String observacoes;
}
