package com.gabrielflorencco.hotelimpact.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Formulário de reserva. Não há campo de identificação nem de estado: nesta
 * fatia reservas não são editadas (datas e valores são históricos) e o estado
 * só muda pelas operações explícitas de confirmação e cancelamento. O código,
 * o total e o estado inicial são sempre definidos pelo servidor.
 *
 * A consistência entre as datas (check-out após check-in), a justificativa
 * para datas passadas e o limite do desconto são validados na camada de
 * serviço, pois dependem de valores combinados e do contexto do negócio.
 */
@Getter
@Setter
public class ReservaForm {

    /** Presente apenas na edição (action do formulário); nulo na criação. */
    private Long id;

    @NotNull
    private Long hospedeId;

    @NotNull
    private Long quartoId;

    /** ISO no input date (yyyy-MM-dd): qualquer outro formato é esvaziado pelo navegador no re-render. */
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataCheckIn;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataCheckOut;

    @NotNull
    @Min(1)
    @Max(20)
    private Integer quantidadeHospedes;

    /** Diária acordada no momento da reserva (valor histórico). */
    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal valorDiaria;

    /** Desconto opcional; nunca pode exceder o valor bruto (validado no serviço). */
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal desconto;

    /** Obrigatório quando a reserva é feita para datas passadas (exceção administrativa). */
    @Size(max = 300)
    private String motivoExcecao;

    @Size(max = 500)
    private String observacoes;
}
