package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
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
}
