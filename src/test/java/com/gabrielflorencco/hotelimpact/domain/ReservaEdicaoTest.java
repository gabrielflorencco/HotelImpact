package com.gabrielflorencco.hotelimpact.domain;

import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Edição controlada de reserva: a entidade valida período e desconto
 * localmente e recalcula o total (diária × noites − desconto). As
 * verificações externas ficam no serviço (BR-RES-009).
 */
class ReservaEdicaoTest {

    private Reserva reservaAtiva() {
        Reserva r = new Reserva();
        r.setCodigo("RES-TEST-000002");
        r.setQuantidadeHospedes(2);
        r.setValorDiaria(new BigDecimal("180.00"));
        r.setDataCheckIn(LocalDate.now());
        r.setDataCheckOut(LocalDate.now().plusDays(2));
        r.setValorTotal(new BigDecimal("360.00"));
        return r;
    }

    @Test
    @DisplayName("Reprogramar recalcula o total com as novas datas e diária")
    void reprogramarRecalculaTotal() {
        Reserva r = reservaAtiva();
        r.reprogramar(null, null,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(8),
                3, new BigDecimal("200.00"), new BigDecimal("100.00"), null, "observação");

        assertThat(r.getValorBruto()).isEqualByComparingTo("600.00");  // 3 noites × 200
        assertThat(r.getValorTotal()).isEqualByComparingTo("500.00"); // − desconto
        assertThat(r.getQuantidadeHospedes()).isEqualTo(3);
        assertThat(r.getObservacoes()).isEqualTo("observação");
    }

    @Test
    @DisplayName("Reprogramar recusa datas invertidas e desconto acima do bruto")
    void reprogramarRecusaEntradasInvalidas() {
        Reserva r = reservaAtiva();
        assertThatThrownBy(() -> r.reprogramar(null, null,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(5),
                2, new BigDecimal("180.00"), null, null, null))
                .isInstanceOf(NegocioException.class)
                .hasMessage("A data de check-out deve ser posterior à data de check-in.");

        assertThatThrownBy(() -> r.reprogramar(null, null,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(7),
                2, new BigDecimal("180.00"), new BigDecimal("999.00"), null, null))
                .isInstanceOf(NegocioException.class)
                .hasMessage("O desconto não pode exceder o valor bruto da reserva (R$ 360.00).");
    }

    @Test
    @DisplayName("Desconto nulo vira zero e o total acompanha o bruto")
    void descontoNuloViraZero() {
        Reserva r = reservaAtiva();
        r.reprogramar(null, null,
                LocalDate.now(), LocalDate.now().plusDays(1),
                2, new BigDecimal("250.00"), null, null, null);
        assertThat(r.getValorTotal()).isEqualByComparingTo("250.00");
    }
}
