package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Hospedagem exibida em listagens: dados já buscados com JOIN FETCH no
 * repositório (reserva, hóspede e quarto), sem consultas adicionais por linha.
 */
@Getter
public class HospedagemResumoView {

    private final Long id;
    private final String reservaCodigo;
    private final String hospedeNome;
    private final String quartoNumero;
    private final LocalDateTime checkInReal;
    private final LocalDateTime checkOutReal;
    private final EstadoHospedagem estado;

    private HospedagemResumoView(Hospedagem hospedagem) {
        this.id = hospedagem.getId();
        this.reservaCodigo = hospedagem.getReserva().getCodigo();
        this.hospedeNome = hospedagem.getReserva().getHospede().getNome();
        this.quartoNumero = hospedagem.getQuarto().getNumero();
        this.checkInReal = hospedagem.getCheckInReal();
        this.checkOutReal = hospedagem.getCheckOutReal();
        this.estado = hospedagem.getEstado();
    }

    public static HospedagemResumoView de(Hospedagem hospedagem) {
        return new HospedagemResumoView(hospedagem);
    }
}
