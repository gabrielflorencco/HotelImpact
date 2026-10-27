package com.gabrielflorencco.hotelimpact.domain;

import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Despacho dinâmico entre constantes de {@link EstadoQuarto}: cada estado
 * responde com o próprio comportamento de recepção, sem condicionais no
 * chamador — o mesmo contrato que o check-in real usa
 * (HospedagemService.validarElegibilidadeCheckIn).
 */
class EstadoQuartoRecepcaoTest {

    private Quarto quarto(EstadoQuarto estado) {
        Quarto q = new Quarto();
        q.setNumero("101");
        q.setEstado(estado);
        return q;
    }

    @Test
    @DisplayName("Cada constante responde o check-in com o próprio comportamento")
    void cadaEstadoRespondePeloContratoDoTipoBase() {
        // Tipo-base em mãos: o chamador não conhece a constante concreta.
        assertThatCode(() -> quarto(EstadoQuarto.DISPONIVEL).getEstado().exigirRecepcao(quarto(EstadoQuarto.DISPONIVEL)))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> quarto(EstadoQuarto.LIMPEZA).getEstado().exigirRecepcao(quarto(EstadoQuarto.LIMPEZA)))
                .hasMessage("O quarto 101 está aguardando limpeza.");

        assertThatThrownBy(() -> quarto(EstadoQuarto.OCUPADO).getEstado().exigirRecepcao(quarto(EstadoQuarto.OCUPADO)))
                .hasMessage("O quarto 101 está ocupado por outra hospedagem.");

        assertThatThrownBy(() -> quarto(EstadoQuarto.MANUTENCAO).getEstado().exigirRecepcao(quarto(EstadoQuarto.MANUTENCAO)))
                .hasMessage("O quarto 101 está fora de operação (MANUTENCAO) e não pode receber hospedagens.");

        assertThatThrownBy(() -> quarto(EstadoQuarto.INTERDITADO).getEstado().exigirRecepcao(quarto(EstadoQuarto.INTERDITADO)))
                .hasMessage("O quarto 101 está fora de operação (INTERDITADO) e não pode receber hospedagens.")
                .isInstanceOf(NegocioException.class);
    }
}
