package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Hospedagem: a ocupação real do quarto, originada de uma reserva (1:1).
 * Registra datas e horas reais de check-in e check-out, independentes das
 * datas previstas na reserva.
 */
@Entity
@Table(name = "hospedagem", indexes = {
        @Index(name = "idx_hospedagem_quarto", columnList = "quarto_id"),
        @Index(name = "idx_hospedagem_estado", columnList = "estado")
})
@Getter
@Setter
public class Hospedagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false, unique = true)
    private Reserva reserva;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Quarto quarto;

    @NotNull
    @Column(name = "check_in_real", nullable = false)
    private LocalDateTime checkInReal;

    @Column(name = "check_out_real")
    private LocalDateTime checkOutReal;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoHospedagem estado = EstadoHospedagem.EM_ANDAMENTO;

    /** Motivo obrigatório no encerramento excepcional com saldo pendente. */
    @Size(max = 300)
    @Column(length = 300)
    private String motivoExcecaoEncerramento;

    @Size(max = 500)
    @Column(length = 500)
    private String observacoes;

    @Version
    private Long versao;

    public boolean isEmAndamento() {
        return estado == EstadoHospedagem.EM_ANDAMENTO;
    }
}
