package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
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

    /*
     * Máquina de estados operacional do quarto (BR-QRT-003): transições só
     * por estas operações de domínio. Verificações que dependem do mundo
     * externo (hospedagem ativa, ordem de manutenção) ficam nos serviços,
     * que chamam estes métodos.
     */

    /** Ocupa o quarto no check-in (elegibilidade validada pelo serviço). */
    public void ocupar() {
        if (estado != EstadoQuarto.DISPONIVEL) {
            throw new NegocioException("Somente quartos disponíveis podem ser ocupados.");
        }
        estado = EstadoQuarto.OCUPADO;
    }

    /** Envia o quarto para limpeza no check-out. */
    public void enviarParaLimpeza() {
        if (estado != EstadoQuarto.OCUPADO) {
            throw new NegocioException("Somente quartos ocupados podem ser enviados para limpeza.");
        }
        estado = EstadoQuarto.LIMPEZA;
    }

    /** Conclui a limpeza pós-check-out e devolve o quarto à operação. */
    public void concluirLimpeza() {
        if (estado != EstadoQuarto.LIMPEZA) {
            throw new NegocioException("Somente quartos aguardando limpeza podem ser liberados.");
        }
        estado = EstadoQuarto.DISPONIVEL;
    }

    /** Interdição administrativa ou por ordem crítica (serviço garante ausência de hospedagem ativa). */
    public void interditar() {
        estado = EstadoQuarto.INTERDITADO;
    }

    /** Libera uma interdição administrativa. */
    public void liberarInterdicao() {
        if (estado != EstadoQuarto.INTERDITADO) {
            throw new NegocioException("Somente quartos interditados podem ser liberados.");
        }
        estado = EstadoQuarto.DISPONIVEL;
    }

    /** Tira o quarto de operação para manutenção (chamado quando a ordem inicia). */
    public void iniciarManutencao() {
        if (estado != EstadoQuarto.DISPONIVEL && estado != EstadoQuarto.LIMPEZA) {
            throw new NegocioException("Somente quartos disponíveis ou em limpeza podem entrar em manutenção.");
        }
        estado = EstadoQuarto.MANUTENCAO;
    }

    /** Restaura a operação após manutenção/interdição da própria ordem (pré-condição garantida pelo chamador). */
    public void disponibilizar() {
        estado = EstadoQuarto.DISPONIVEL;
    }
}
