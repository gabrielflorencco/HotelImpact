package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
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

    /** Início da estadia: registra o check-in e abre a hospedagem (BR-HSP-001). */
    public void abrir(java.time.LocalDateTime checkInReal) {
        this.checkInReal = checkInReal;
        this.estado = EstadoHospedagem.EM_ANDAMENTO;
    }

    /*
     * Encerramento da hospedagem (BR-HSP-002): a própria hospedagem valida
     * seu estado e o saldo da conta — o serviço calcula o saldo (consulta
     * lançamentos) e orquestra reserva e quarto na mesma transação.
     */

    /**
     * Encerramento normal: exige hospedagem em andamento e conta quitada.
     * Sobrecarga de negócio — o caminho sem justificativa só aceita saldo zero.
     */
    public void encerrar(java.math.BigDecimal saldoDevedor) {
        if (estado != EstadoHospedagem.EM_ANDAMENTO) {
            throw new NegocioException("Somente hospedagens em andamento podem ser encerradas.");
        }
        if (saldoDevedor.signum() > 0) {
            throw new NegocioException("A conta possui saldo pendente de R$ "
                    + saldoDevedor.toPlainString() + ". Informe a justificativa do encerramento excepcional.");
        }
        estado = EstadoHospedagem.ENCERRADA;
        checkOutReal = java.time.LocalDateTime.now();
    }

    /**
     * Encerramento com justificativa: com saldo pendente, exige motivo e
     * caracteriza o encerramento excepcional (hospedagem CANCELADA com o
     * motivo registrado); com conta quitada, encerra normalmente.
     */
    public void encerrar(java.math.BigDecimal saldoDevedor, String motivoExcecao) {
        if (estado != EstadoHospedagem.EM_ANDAMENTO) {
            throw new NegocioException("Somente hospedagens em andamento podem ser encerradas.");
        }
        if (saldoDevedor.signum() > 0) {
            if (motivoExcecao == null || motivoExcecao.isBlank()) {
                throw new NegocioException("A conta possui saldo pendente de R$ "
                        + saldoDevedor.toPlainString() + ". Informe a justificativa do encerramento excepcional.");
            }
            motivoExcecaoEncerramento = motivoExcecao.trim();
            estado = EstadoHospedagem.CANCELADA;
        } else {
            estado = EstadoHospedagem.ENCERRADA;
        }
        checkOutReal = java.time.LocalDateTime.now();
    }
}
