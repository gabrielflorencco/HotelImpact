package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Reserva exibida na tela de detalhes, com dados formatados para leitura
 * e a indicação de quais operações de estado ainda são possíveis.
 */
@Getter
public final class ReservaDetalheView {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Long id;
    private final String codigo;
    private final EstadoReserva estado;
    private final boolean ativa;
    private final Long hospedeId;
    private final String hospedeNome;
    private final Long quartoId;
    private final String quartoNumero;
    private final String dataCheckIn;
    private final String dataCheckOut;
    private final long noites;
    private final Integer quantidadeHospedes;
    private final BigDecimal valorDiaria;
    private final BigDecimal valorBruto;
    private final BigDecimal desconto;
    private final BigDecimal valorTotal;
    private final String motivoExcecao;
    private final String observacoes;
    private final String dataCriacao;

    private ReservaDetalheView(Reserva reserva) {
        this.id = reserva.getId();
        this.codigo = reserva.getCodigo();
        this.estado = reserva.getEstado();
        this.ativa = reserva.isAtiva();
        this.hospedeId = reserva.getHospede().getId();
        this.hospedeNome = reserva.getHospede().getNome();
        this.quartoId = reserva.getQuarto().getId();
        this.quartoNumero = reserva.getQuarto().getNumero();
        this.dataCheckIn = reserva.getDataCheckIn().format(DATA);
        this.dataCheckOut = reserva.getDataCheckOut().format(DATA);
        this.noites = reserva.getQuantidadeNoites();
        this.quantidadeHospedes = reserva.getQuantidadeHospedes();
        this.valorDiaria = reserva.getValorDiaria();
        this.valorBruto = reserva.getValorBruto();
        this.desconto = reserva.getDesconto();
        this.valorTotal = reserva.getValorTotal();
        this.motivoExcecao = reserva.getMotivoExcecao();
        this.observacoes = reserva.getObservacoes();
        this.dataCriacao = reserva.getDataCriacao() == null ? null : reserva.getDataCriacao().format(DATA_HORA);
    }

    public static ReservaDetalheView de(Reserva reserva) {
        return new ReservaDetalheView(reserva);
    }
}
