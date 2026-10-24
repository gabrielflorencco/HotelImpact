package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Hospedagem exibida no relatório por período: dados já buscados com
 * JOIN FETCH no repositório, sem consultas adicionais por linha.
 */
@Getter
public class HospedagemRelatorioView {

    private final Long id;
    private final String reservaCodigo;
    private final String hospedeNome;
    private final String quartoNumero;
    private final LocalDateTime checkInReal;
    private final LocalDateTime checkOutReal;
    private final EstadoHospedagem estado;
    private final BigDecimal valorTotalReserva;

    private HospedagemRelatorioView(Hospedagem hospedagem) {
        this.id = hospedagem.getId();
        this.reservaCodigo = hospedagem.getReserva().getCodigo();
        this.hospedeNome = hospedagem.getReserva().getHospede().getNome();
        this.quartoNumero = hospedagem.getQuarto().getNumero();
        this.checkInReal = hospedagem.getCheckInReal();
        this.checkOutReal = hospedagem.getCheckOutReal();
        this.estado = hospedagem.getEstado();
        this.valorTotalReserva = hospedagem.getReserva().getValorTotal();
    }

    public static HospedagemRelatorioView de(Hospedagem hospedagem) {
        return new HospedagemRelatorioView(hospedagem);
    }
}
