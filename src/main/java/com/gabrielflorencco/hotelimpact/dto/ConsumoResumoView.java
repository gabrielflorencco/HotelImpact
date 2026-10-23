package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.Consumo;
import com.gabrielflorencco.hotelimpact.enums.EstadoConsumo;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Lançamento de consumo exibido na conta da hospedagem: preço unitário
 * histórico capturado no lançamento, total calculado no servidor e o
 * estado que distingue o estorno lógico (CANCELADO) do lançamento ativo.
 */
@Getter
public class ConsumoResumoView {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Long id;
    private final String itemNome;
    private final TipoItem tipo;
    private final Integer quantidade;
    private final BigDecimal precoUnitario;
    private final BigDecimal valorTotal;
    private final String dataHora;
    private final EstadoConsumo estado;
    private final String observacoes;

    private ConsumoResumoView(Consumo consumo) {
        this.id = consumo.getId();
        this.itemNome = consumo.getItem().getNome();
        this.tipo = consumo.getItem().getTipo();
        this.quantidade = consumo.getQuantidade();
        this.precoUnitario = consumo.getPrecoUnitario();
        this.valorTotal = consumo.getValorTotal();
        this.dataHora = consumo.getDataHora() == null ? null : consumo.getDataHora().format(DATA_HORA);
        this.estado = consumo.getEstado();
        this.observacoes = consumo.getObservacoes();
    }

    public static ConsumoResumoView de(Consumo consumo) {
        return new ConsumoResumoView(consumo);
    }
}
