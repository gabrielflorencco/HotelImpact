package com.gabrielflorencco.hotelimpact.enums;

import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;

/**
 * Estado operacional do quarto. Diferente da disponibilidade para um intervalo de datas,
 * que é calculada a partir das reservas e hospedagens existentes.
 *
 * Cada constante sabe o que significa receber um hóspede: este é o
 * comportamento de recepção do estado, despachado dinamicamente no
 * check-in (BR-HSP-001) — o serviço não pergunta "qual estado é este?",
 * apenas delega ao próprio estado.
 */
public enum EstadoQuarto {
    DISPONIVEL {
        /** Quarto pronto: a validação prossegue. */
        @Override
        public void exigirRecepcao(Quarto quarto) {
            /* quarto pronto para receber o hóspede */
        }
    },
    OCUPADO {
        @Override
        public void exigirRecepcao(Quarto quarto) {
            throw new NegocioException(
                    "O quarto " + quarto.getNumero() + " está ocupado por outra hospedagem.");
        }
    },
    LIMPEZA {
        @Override
        public void exigirRecepcao(Quarto quarto) {
            throw new NegocioException(
                    "O quarto " + quarto.getNumero() + " está aguardando limpeza.");
        }
    },
    MANUTENCAO {
        @Override
        public void exigirRecepcao(Quarto quarto) {
            recusarForaDeOperacao(quarto);
        }
    },
    INTERDITADO {
        @Override
        public void exigirRecepcao(Quarto quarto) {
            recusarForaDeOperacao(quarto);
        }
    };

    /** Elegibilidade de check-in: o estado decide se o quarto pode receber o hóspede. */
    public abstract void exigirRecepcao(Quarto quarto);

    private static void recusarForaDeOperacao(Quarto quarto) {
        throw new NegocioException(
                "O quarto " + quarto.getNumero() + " está fora de operação (" + quarto.getEstado()
                        + ") e não pode receber hospedagens.");
    }
}
