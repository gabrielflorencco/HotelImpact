package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.OrdemManutencao;
import com.gabrielflorencco.hotelimpact.enums.EstadoManutencao;
import com.gabrielflorencco.hotelimpact.enums.PrioridadeManutencao;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

/**
 * Ordem de manutenção exibida na tela de detalhes, com datas formatadas e
 * as operações de estado possíveis (iniciar/concluir/cancelar).
 */
@Getter
public class OrdemManutencaoDetalheView {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Long id;
    private final Long quartoId;
    private final String quartoNumero;
    private final String titulo;
    private final String descricao;
    private final PrioridadeManutencao prioridade;
    private final EstadoManutencao estado;
    private final boolean interditouQuarto;
    private final String abertaEm;
    private final String concluidaEm;
    private final String observacoes;
    private final boolean podeIniciar;
    private final boolean podeConcluirOuCancelar;

    private OrdemManutencaoDetalheView(OrdemManutencao ordem) {
        this.id = ordem.getId();
        this.quartoId = ordem.getQuarto().getId();
        this.quartoNumero = ordem.getQuarto().getNumero();
        this.titulo = ordem.getTitulo();
        this.descricao = ordem.getDescricao();
        this.prioridade = ordem.getPrioridade();
        this.estado = ordem.getEstado();
        this.interditouQuarto = ordem.getInterditouQuarto();
        this.abertaEm = ordem.getAbertaEm() == null ? null : ordem.getAbertaEm().format(DATA_HORA);
        this.concluidaEm = ordem.getConcluidaEm() == null ? null : ordem.getConcluidaEm().format(DATA_HORA);
        this.observacoes = ordem.getObservacoes();
        this.podeIniciar = ordem.getEstado() == EstadoManutencao.ABERTA;
        this.podeConcluirOuCancelar = ordem.getEstado() == EstadoManutencao.ABERTA
                || ordem.getEstado() == EstadoManutencao.EM_ANDAMENTO;
    }

    public static OrdemManutencaoDetalheView de(OrdemManutencao ordem) {
        return new OrdemManutencaoDetalheView(ordem);
    }
}
