package com.gabrielflorencco.hotelimpact.domain;

import com.gabrielflorencco.hotelimpact.entity.TarefaLimpeza;
import com.gabrielflorencco.hotelimpact.enums.EstadoTarefaLimpeza;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Máquina de estados da tarefa de limpeza: PENDENTE → EM_EXECUCAO →
 * CONCLUIDA. Tarefa concluída é histórico — nunca reinicia nem conclui de novo.
 */
class TarefaLimpezaTransicoesTest {

    @Test
    @DisplayName("Tarefa segue o ciclo pendente → em execução → concluída com data registrada")
    void tarefaSegueSeuCiclo() {
        TarefaLimpeza tarefa = new TarefaLimpeza();
        assertThat(tarefa.getEstado()).isEqualTo(EstadoTarefaLimpeza.PENDENTE);
        assertThat(tarefa.aberta()).isTrue();

        tarefa.iniciar();
        assertThat(tarefa.getEstado()).isEqualTo(EstadoTarefaLimpeza.EM_EXECUCAO);

        tarefa.concluir();
        assertThat(tarefa.getEstado()).isEqualTo(EstadoTarefaLimpeza.CONCLUIDA);
        assertThat(tarefa.getConcluidaEm()).isNotNull();
        assertThat(tarefa.aberta()).isFalse();
    }

    @Test
    @DisplayName("Tarefa concluída é histórico imutável: não reinicia e não conclui de novo")
    void tarefaConcluidaNaoTransicionaNovamente() {
        TarefaLimpeza concluida = new TarefaLimpeza();
        concluida.iniciar();
        concluida.concluir();

        assertThatThrownBy(concluida::iniciar)
                .isInstanceOf(NegocioException.class)
                .hasMessage("Somente tarefas de limpeza pendentes podem ser iniciadas.");
        assertThatThrownBy(concluida::concluir)
                .isInstanceOf(NegocioException.class)
                .hasMessage("A tarefa de limpeza já foi concluída.");
    }

    @Test
    @DisplayName("Tarefa em execução não pode ser iniciada novamente")
    void tarefaEmExecucaoNaoReinicia() {
        TarefaLimpeza emExecucao = new TarefaLimpeza();
        emExecucao.iniciar();
        assertThatThrownBy(emExecucao::iniciar)
                .hasMessage("Somente tarefas de limpeza pendentes podem ser iniciadas.");
        // mas pode ser concluída direto (governança pode terminar sem registrar início)
        emExecucao.concluir();
        assertThat(emExecucao.getEstado()).isEqualTo(EstadoTarefaLimpeza.CONCLUIDA);
    }
}
