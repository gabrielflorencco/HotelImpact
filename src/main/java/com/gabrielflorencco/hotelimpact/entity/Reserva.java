package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Reserva: o compromisso futuro de hospedagem. Períodos são intervalos semiabertos
 * [check-in, check-out). O valor da diária e o total são históricos — mudanças no
 * preço-base da categoria não afetam reservas existentes. O total e o bruto são
 * sempre calculados no backend.
 */
@Entity
@Table(name = "reserva", indexes = {
        @Index(name = "idx_reserva_quarto_periodo", columnList = "quarto_id, data_check_in"),
        @Index(name = "idx_reserva_estado", columnList = "estado"),
        @Index(name = "idx_reserva_check_in", columnList = "data_check_in")
})
@Getter
@Setter
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código legível único gerado pelo sistema (ex.: RES-2026-000123). */
    @NotNull
    @Size(max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Hospede hospede;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Quarto quarto;

    @NotNull
    @Column(name = "data_check_in", nullable = false)
    private LocalDate dataCheckIn;

    @NotNull
    @Column(name = "data_check_out", nullable = false)
    private LocalDate dataCheckOut;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantidadeHospedes;

    /** Diária acordada no momento da reserva (valor histórico). */
    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDiaria;

    /** Desconto não negativo, limitado ao valor bruto (validado na camada de serviço). */
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    /** Total calculado no backend: (noites × diária) − desconto. */
    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado = EstadoReserva.PENDENTE;

    /** Justificativa obrigatória quando a reserva é feita para datas passadas (exceção administrativa). */
    @Size(max = 300)
    @Column(length = 300)
    private String motivoExcecao;

    @Size(max = 500)
    @Column(length = 500)
    private String observacoes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    /** Versionamento otimista: duas reservas simultâneas para o mesmo quarto/período. */
    @Version
    private Long versao;

    /** Número de noites do período semiaberto [check-in, check-out). */
    public long getQuantidadeNoites() {
        return ChronoUnit.DAYS.between(dataCheckIn, dataCheckOut);
    }

    /** Valor bruto da hospedagem sem desconto. */
    public BigDecimal getValorBruto() {
        return valorDiaria.multiply(BigDecimal.valueOf(getQuantidadeNoites()));
    }

    /** Reservas em estado que bloqueia a disponibilidade do quarto. */
    public boolean isAtiva() {
        return estado == EstadoReserva.PENDENTE || estado == EstadoReserva.CONFIRMADA;
    }

    /*
     * Máquina de estados da reserva (BR-RES-008): a própria reserva é a
     * única capaz de transitar seu estado. Os serviços orquestram as
     * verificações que dependem do mundo externo (check-in existente,
     * data passada, persistência) e chamam estas operações de domínio.
     */

    /** Confirma uma reserva pendente. */
    public void confirmar() {
        if (estado != EstadoReserva.PENDENTE) {
            throw new NegocioException("Somente reservas pendentes podem ser confirmadas.");
        }
        estado = EstadoReserva.CONFIRMADA;
    }

    /** Cancela uma reserva ativa — o serviço garante que ainda não houve check-in. */
    public void cancelar() {
        if (!isAtiva()) {
            throw new NegocioException("Somente reservas pendentes ou confirmadas podem ser canceladas.");
        }
        estado = EstadoReserva.CANCELADA;
    }

    /** Registra o não-comparecimento — o serviço garante data de chegada passada e ausência de check-in. */
    public void registrarNoShow() {
        if (!isAtiva()) {
            throw new NegocioException("Somente reservas pendentes ou confirmadas podem ser registradas como no-show.");
        }
        estado = EstadoReserva.NO_SHOW;
    }

    /** Conclui a reserva no check-out da hospedagem. */
    public void concluir() {
        if (!isAtiva()) {
            throw new NegocioException("Somente reservas ativas podem ser concluídas.");
        }
        estado = EstadoReserva.CONCLUIDA;
    }
}
