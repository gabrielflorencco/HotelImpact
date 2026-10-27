package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Hospedagem exibida na tela de detalhes, com datas reais e previstas
 * formatadas para leitura e o total histórico da reserva.
 */
@Getter
public final class HospedagemDetalheView {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Long id;
    private final Long reservaId;
    private final String reservaCodigo;
    private final EstadoHospedagem estado;
    private final Long hospedeId;
    private final String hospedeNome;
    private final String quartoNumero;
    private final String checkInReal;
    private final String checkOutReal;
    private final String periodoPrevisto;
    private final BigDecimal valorTotalReserva;
    private final String observacoes;

    private HospedagemDetalheView(Hospedagem hospedagem) {
        this.id = hospedagem.getId();
        this.reservaId = hospedagem.getReserva().getId();
        this.reservaCodigo = hospedagem.getReserva().getCodigo();
        this.estado = hospedagem.getEstado();
        this.hospedeId = hospedagem.getReserva().getHospede().getId();
        this.hospedeNome = hospedagem.getReserva().getHospede().getNome();
        this.quartoNumero = hospedagem.getQuarto().getNumero();
        this.checkInReal = hospedagem.getCheckInReal() == null ? null : hospedagem.getCheckInReal().format(DATA_HORA);
        this.checkOutReal = hospedagem.getCheckOutReal() == null ? null : hospedagem.getCheckOutReal().format(DATA_HORA);
        this.periodoPrevisto = hospedagem.getReserva().getDataCheckIn().format(DATA) + " a "
                + hospedagem.getReserva().getDataCheckOut().format(DATA);
        this.valorTotalReserva = hospedagem.getReserva().getValorTotal();
        this.observacoes = hospedagem.getObservacoes();
    }

    public static HospedagemDetalheView de(Hospedagem hospedagem) {
        return new HospedagemDetalheView(hospedagem);
    }
}
