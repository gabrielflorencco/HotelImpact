package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.OrdemManutencao;
import com.gabrielflorencco.hotelimpact.enums.EstadoManutencao;
import com.gabrielflorencco.hotelimpact.enums.PrioridadeManutencao;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Ordem de manutenção exibida em listagens: quarto já buscado com JOIN
 * FETCH no repositório, sem consultas adicionais por linha.
 */
@Getter
public class OrdemManutencaoResumoView {

    private final Long id;
    private final String quartoNumero;
    private final String titulo;
    private final PrioridadeManutencao prioridade;
    private final EstadoManutencao estado;
    private final boolean interditouQuarto;
    private final LocalDateTime abertaEm;
    private final LocalDateTime concluidaEm;

    private OrdemManutencaoResumoView(OrdemManutencao ordem) {
        this.id = ordem.getId();
        this.quartoNumero = ordem.getQuarto().getNumero();
        this.titulo = ordem.getTitulo();
        this.prioridade = ordem.getPrioridade();
        this.estado = ordem.getEstado();
        this.interditouQuarto = ordem.getInterditouQuarto();
        this.abertaEm = ordem.getAbertaEm();
        this.concluidaEm = ordem.getConcluidaEm();
    }

    public static OrdemManutencaoResumoView de(OrdemManutencao ordem) {
        return new OrdemManutencaoResumoView(ordem);
    }
}
