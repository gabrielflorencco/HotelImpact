package com.gabrielflorencco.hotelimpact.domain;

import com.gabrielflorencco.hotelimpact.entity.Consumo;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.OrdemManutencao;
import com.gabrielflorencco.hotelimpact.entity.Pagamento;
import com.gabrielflorencco.hotelimpact.enums.EstadoConsumo;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoManutencao;
import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Máquinas de estado de Hospedagem, OrdemManutencao, Consumo e Pagamento —
 * incluindo a sobrecarga de negócio {@code Hospedagem.encerrar(...)}: as
 * duas assinaturas têm semântica distinta (encerramento normal exige conta
 * quitada; encerramento com justificativa permite saldo pendente).
 */
class TransicoesDeEstadoIITest {

    private Hospedagem emAndamento() {
        Hospedagem h = new Hospedagem();
        h.abrir(java.time.LocalDateTime.now());
        return h;
    }

    // ---- Sobrecarga encerrar (BR-HSP-002) ----

    @Test
    @DisplayName("encerrar(saldo) fecha hospedagem quitada e recusa saldo pendente sem justificativa")
    void encerramentoNormalExigeContaQuitada() {
        Hospedagem quitada = emAndamento();
        quitada.encerrar(new BigDecimal("0.00"));
        assertThat(quitada.getEstado()).isEqualTo(EstadoHospedagem.ENCERRADA);
        assertThat(quitada.getCheckOutReal()).isNotNull();

        Hospedagem devedora = emAndamento();
        assertThatThrownBy(() -> devedora.encerrar(new BigDecimal("150.00")))
                .isInstanceOf(NegocioException.class)
                .hasMessage("A conta possui saldo pendente de R$ 150.00. Informe a justificativa do encerramento excepcional.");
        assertThat(devedora.getEstado()).isEqualTo(EstadoHospedagem.EM_ANDAMENTO);
    }

    @Test
    @DisplayName("encerrar(saldo, motivo) permite o encerramento excepcional com justificativa registrada")
    void encerramentoExcepcionalRegistraJustificativa() {
        Hospedagem devedora = emAndamento();
        devedora.encerrar(new BigDecimal("150.00"), "  hóspede fugiu sem pagar  ");
        assertThat(devedora.getEstado()).isEqualTo(EstadoHospedagem.CANCELADA);
        assertThat(devedora.getMotivoExcecaoEncerramento()).isEqualTo("hóspede fugiu sem pagar");
        assertThat(devedora.getCheckOutReal()).isNotNull();

        // com conta quitada, a justificativa é desnecessária: encerra normal
        Hospedagem quitada = emAndamento();
        quitada.encerrar(BigDecimal.ZERO, null);
        assertThat(quitada.getEstado()).isEqualTo(EstadoHospedagem.ENCERRADA);

        // saldo pendente sem motivo na assinatura com justificativa: recusa
        Hospedagem semMotivo = emAndamento();
        assertThatThrownBy(() -> semMotivo.encerrar(new BigDecimal("10.00"), "  "))
                .hasMessageContaining("Informe a justificativa do encerramento excepcional.");
    }

    @Test
    @DisplayName("Somente hospedagens em andamento podem ser encerradas")
    void hospedagemEncerradaNaoReencerra() {
        Hospedagem encerrada = emAndamento();
        encerrada.encerrar(BigDecimal.ZERO);
        assertThatThrownBy(() -> encerrada.encerrar(BigDecimal.ZERO))
                .hasMessage("Somente hospedagens em andamento podem ser encerradas.");
    }

    // ---- Ordem de manutenção (BR-MNT-002) ----

    @Test
    @DisplayName("Ordem transiciona ABERTA → EM_ANDAMENTO → CONCLUIDA e recusa repetições")
    void ordemSegueSuaMaquinaDeEstados() {
        OrdemManutencao ordem = new OrdemManutencao();
        ordem.setEstado(EstadoManutencao.ABERTA);
        ordem.iniciarExecucao();
        assertThat(ordem.getEstado()).isEqualTo(EstadoManutencao.EM_ANDAMENTO);
        assertThatThrownBy(ordem::iniciarExecucao)
                .hasMessage("Somente ordens abertas podem ser iniciadas.");
        ordem.concluirOrdem();
        assertThat(ordem.getEstado()).isEqualTo(EstadoManutencao.CONCLUIDA);
        assertThat(ordem.getConcluidaEm()).isNotNull();
        assertThatThrownBy(() -> ordem.concluirOrdem())
                .hasMessage("Somente ordens abertas ou em andamento permitem esta operação.");
    }

    // ---- Consumo (BR-CON-002) ----

    @Test
    @DisplayName("Consumo cancela uma única vez")
    void consumoCancelaUmaUnicaVez() {
        Consumo consumo = new Consumo();
        consumo.setEstado(EstadoConsumo.ATIVO);
        consumo.cancelar();
        assertThat(consumo.getEstado()).isEqualTo(EstadoConsumo.CANCELADO);
        assertThatThrownBy(consumo::cancelar)
                .hasMessage("O lançamento já está cancelado.");
    }

    // ---- Pagamento (BR-PAG-002) ----

    @Test
    @DisplayName("Pagamento estorna uma única vez")
    void pagamentoEstornaUmaUnicaVez() {
        Pagamento pagamento = new Pagamento();
        pagamento.setEstado(EstadoPagamento.CONFIRMADO);
        pagamento.estornar();
        assertThat(pagamento.getEstado()).isEqualTo(EstadoPagamento.ESTORNADO);
        assertThatThrownBy(pagamento::estornar)
                .hasMessage("O pagamento já está estornado.");
    }
}
