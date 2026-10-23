package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reserva exibida em listagens: dados já buscados com JOIN FETCH no
 * repositório (hóspede e quarto), sem consultas adicionais por linha.
 */
@Getter
public class ReservaResumoView {

    private final Long id;
    private final String codigo;
    private final String hospedeNome;
    private final String quartoNumero;
    private final LocalDate dataCheckIn;
    private final LocalDate dataCheckOut;
    private final long noites;
    private final BigDecimal valorTotal;
    private final EstadoReserva estado;

    private ReservaResumoView(Reserva reserva) {
        this.id = reserva.getId();
        this.codigo = reserva.getCodigo();
        this.hospedeNome = reserva.getHospede().getNome();
        this.quartoNumero = reserva.getQuarto().getNumero();
        this.dataCheckIn = reserva.getDataCheckIn();
        this.dataCheckOut = reserva.getDataCheckOut();
        this.noites = reserva.getQuantidadeNoites();
        this.valorTotal = reserva.getValorTotal();
        this.estado = reserva.getEstado();
    }

    public static ReservaResumoView de(Reserva reserva) {
        return new ReservaResumoView(reserva);
    }
}
