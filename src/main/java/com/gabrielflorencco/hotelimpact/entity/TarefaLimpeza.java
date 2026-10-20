package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.enums.EstadoTarefaLimpeza;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tarefa de limpeza de um quarto. O histórico de tarefas concluídas é preservado.
 */
@Entity
@Table(name = "tarefa_limpeza", indexes = {
        @Index(name = "idx_tarefa_limpeza_quarto", columnList = "quarto_id"),
        @Index(name = "idx_tarefa_limpeza_estado", columnList = "estado")
})
@Getter
@Setter
public class TarefaLimpeza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Quarto quarto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoTarefaLimpeza estado = EstadoTarefaLimpeza.PENDENTE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadaEm;

    @Column
    private LocalDateTime concluidaEm;

    @Size(max = 300)
    @Column(length = 300)
    private String observacoes;
}
