package com.gabrielflorencco.hotelimpact.domain;

import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Máquinas de estado do domínio, exercitadas diretamente nas entidades —
 * o objeto recusa transições inválidas sem depender de serviço, Spring
 * ou banco. As mensagens são as mesmas dos fluxos reais (testes de
 * integração cobrem o caminho completo).
 */
class TransicoesDeEstadoTest {

    private Reserva reserva(EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setCodigo("RES-TEST-000001");
        r.setQuantidadeHospedes(2);
        r.setValorDiaria(new BigDecimal("180.00"));
        r.setDataCheckIn(LocalDate.now());
        r.setDataCheckOut(LocalDate.now().plusDays(1));
        r.setEstado(estado);
        return r;
    }

    private Quarto quarto(EstadoQuarto estado) {
        CategoriaQuarto standard = new CategoriaQuarto();
        standard.setNome("Standard");
        standard.setCapacidadeMaxima(2);
        Quarto q = new Quarto();
        q.setNumero("101");
        q.setAndar(1);
        q.setCategoria(standard);
        q.setEstado(estado);
        return q;
    }

    // ---- Reserva (BR-RES-008) ----

    @Test
    @DisplayName("Reserva pendente pode ser confirmada; confirmada não pode novamente")
    void reservaPendenteConfirmaMasConfirmadaNaoReconfirma() {
        Reserva pendente = reserva(EstadoReserva.PENDENTE);
        pendente.confirmar();
        assertThat(pendente.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);

        assertThatThrownBy(pendente::confirmar)
                .isInstanceOf(NegocioException.class)
                .hasMessage("Somente reservas pendentes podem ser confirmadas.");
    }

    @Test
    @DisplayName("Somente reservas ativas podem ser canceladas, registradas como no-show ou concluídas")
    void somenteReservasAtivasTransicionam() {
        Reserva concluida = reserva(EstadoReserva.CONCLUIDA);
        assertThatThrownBy(concluida::cancelar)
                .hasMessage("Somente reservas pendentes ou confirmadas podem ser canceladas.");
        assertThatThrownBy(concluida::registrarNoShow)
                .hasMessage("Somente reservas pendentes ou confirmadas podem ser registradas como no-show.");
        assertThatThrownBy(concluida::concluir)
                .hasMessage("Somente reservas ativas podem ser concluídas.");

        Reserva confirmada = reserva(EstadoReserva.CONFIRMADA);
        confirmada.cancelar();
        assertThat(confirmada.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    // ---- Quarto (BR-QRT-003) ----

    @Test
    @DisplayName("Ciclo operacional completo do quarto fica em mãos da própria entidade")
    void cicloOperacionalDoQuarto() {
        Quarto q = quarto(EstadoQuarto.DISPONIVEL);
        q.ocupar();
        assertThat(q.getEstado()).isEqualTo(EstadoQuarto.OCUPADO);
        q.enviarParaLimpeza();
        assertThat(q.getEstado()).isEqualTo(EstadoQuarto.LIMPEZA);
        q.concluirLimpeza();
        assertThat(q.getEstado()).isEqualTo(EstadoQuarto.DISPONIVEL);
        q.iniciarManutencao();
        assertThat(q.getEstado()).isEqualTo(EstadoQuarto.MANUTENCAO);
        q.disponibilizar();
        assertThat(q.getEstado()).isEqualTo(EstadoQuarto.DISPONIVEL);
        q.interditar();
        assertThat(q.getEstado()).isEqualTo(EstadoQuarto.INTERDITADO);
        q.liberarInterdicao();
        assertThat(q.getEstado()).isEqualTo(EstadoQuarto.DISPONIVEL);
    }

    @Test
    @DisplayName("Quarto recusa transições fora da máquina de estados")
    void quartoRecusaTransicoesInvalidas() {
        Quarto ocupado = quarto(EstadoQuarto.OCUPADO);
        assertThatThrownBy(ocupado::ocupar)
                .hasMessage("Somente quartos disponíveis podem ser ocupados.");
        assertThatThrownBy(ocupado::concluirLimpeza)
                .hasMessage("Somente quartos aguardando limpeza podem ser liberados.");
        assertThatThrownBy(ocupado::liberarInterdicao)
                .hasMessage("Somente quartos interditados podem ser liberados.");

        Quarto interditado = quarto(EstadoQuarto.INTERDITADO);
        assertThatThrownBy(interditado::iniciarManutencao)
                .hasMessage("Somente quartos disponíveis ou em limpeza podem entrar em manutenção.");
    }
}
