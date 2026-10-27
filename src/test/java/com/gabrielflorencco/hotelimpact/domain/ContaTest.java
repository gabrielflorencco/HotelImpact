package com.gabrielflorencco.hotelimpact.domain;

import com.gabrielflorencco.hotelimpact.entity.Consumo;
import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.entity.Pagamento;
import com.gabrielflorencco.hotelimpact.enums.EstadoConsumo;
import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import com.gabrielflorencco.hotelimpact.enums.FormaPagamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A conta da hospedagem como objeto de valor — a cadeia completa das
 * abstrações de domínio em um fluxo real de fechamento:
 *
 * <ul>
 *   <li>contrato {@link Lancamento} com duas implementações de comportamento
 *       distinto (Consumo conta se ATIVO; Pagamento conta se CONFIRMADO);</li>
 *   <li>cálculo genérico {@link Fechamento#totalElegivel} sobre listas
 *       concretamente tipadas (polimorfismo paramétrico, sem casts);</li>
 *   <li>despacho dinâmico: a mesma chamada decide pela classe real
 *       (polimorfismo de subtipos, consumidor só conhece o tipo-base);</li>
 *   <li>imutabilidade do record e fórmulas do negócio (BR-PAG-003/004).</li>
 * </ul>
 */
class ContaTest {

    private Consumo consumo(String valor, EstadoConsumo estado) {
        ItemCatalogo agua = new ItemCatalogo();
        agua.setPrecoUnitario(new BigDecimal(valor));
        Consumo c = new Consumo();
        c.setItem(agua);
        c.setQuantidade(1);
        c.setPrecoUnitario(new BigDecimal(valor));
        c.setValorTotal(new BigDecimal(valor));
        c.setEstado(estado);
        return c;
    }

    private Pagamento pagamento(String valor, EstadoPagamento estado) {
        Pagamento p = new Pagamento();
        p.setValor(new BigDecimal(valor));
        p.setForma(FormaPagamento.PIX);
        p.setEstado(estado);
        return p;
    }

    @Test
    @DisplayName("Contrato Lancamento: consumo e pagamento decidem a própria elegibilidade")
    void contratoLancamentoTemDuasRegrasReaisDistintas() {
        Lancamento consumoAtivo = consumo("5.00", EstadoConsumo.ATIVO);
        Lancamento consumoCancelado = consumo("5.00", EstadoConsumo.CANCELADO);
        Lancamento pagamentoConfirmado = pagamento("200.00", EstadoPagamento.CONFIRMADO);
        Lancamento pagamentoEstornado = pagamento("200.00", EstadoPagamento.ESTORNADO);

        // Referências pelo tipo-base (upcasting): o comportamento é da classe real.
        assertThat(consumoAtivo.contaNoFechamento()).isTrue();
        assertThat(consumoCancelado.contaNoFechamento()).isFalse();     // consumos cancelados saem
        assertThat(pagamentoConfirmado.contaNoFechamento()).isTrue();
        assertThat(pagamentoEstornado.contaNoFechamento()).isFalse();    // estornados não são recebidos
        assertThat(consumoAtivo.valorContabil()).isEqualByComparingTo("5.00");
        assertThat(pagamentoConfirmado.valorContabil()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Fechamento genérico soma listas concretamente tipadas sem casts")
    void fechamentoGenericoSomaQualquerTipoDeLancamento() {
        List<Consumo> consumos = List.of(consumo("5.00", EstadoConsumo.ATIVO),
                consumo("7.00", EstadoConsumo.CANCELADO));
        List<Pagamento> pagamentos = List.of(pagamento("100.00", EstadoPagamento.CONFIRMADO),
                pagamento("50.00", EstadoPagamento.ESTORNADO));

        // O mesmo método aceita List<Consumo> e List<Pagamento> com segurança
        // de tipos em compilação — T preserva o tipo concreto de cada lista.
        assertThat(Fechamento.totalElegivel(consumos)).isEqualByComparingTo("5.00");
        assertThat(Fechamento.totalElegivel(pagamentos)).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Conta sem pagamentos: saldo devedor é o total (BR-PAG-004)")
    void contaSemPagamentosFicaIntegralmenteDevedora() {
        Conta conta = new Conta(new BigDecimal("360.00"), List.of(), List.of());
        assertThat(conta.total()).isEqualByComparingTo("360.00");
        assertThat(conta.pago()).isEqualByComparingTo("0.00");
        assertThat(conta.saldoDevedor()).isEqualByComparingTo("360.00");
        assertThat(conta.quitada()).isFalse();
    }

    @Test
    @DisplayName("Pagamento parcial reduz o saldo; quitado zera")
    void pagamentoParcialReduzSaldoEQuitadoZera() {
        List<Pagamento> parcial = List.of(pagamento("200.00", EstadoPagamento.CONFIRMADO));
        Conta contaParcial = new Conta(new BigDecimal("360.00"), List.of(), parcial);
        assertThat(contaParcial.saldoDevedor()).isEqualByComparingTo("160.00");
        assertThat(contaParcial.quitada()).isFalse();

        List<Pagamento> quitada = List.of(pagamento("360.00", EstadoPagamento.CONFIRMADO));
        Conta contaQuitada = new Conta(new BigDecimal("360.00"), List.of(), quitada);
        assertThat(contaQuitada.saldoDevedor()).isEqualByComparingTo("0.00");
        assertThat(contaQuitada.quitada()).isTrue();
    }

    @Test
    @DisplayName("Consumos contam no total; cancelado e estorno saem da conta")
    void lancamentosInelegiveisNaoEntramNoFechamento() {
        List<Consumo> consumos = List.of(consumo("10.00", EstadoConsumo.ATIVO),
                consumo("25.00", EstadoConsumo.CANCELADO));
        List<Pagamento> pagamentos = List.of(pagamento("400.00", EstadoPagamento.CONFIRMADO),
                pagamento("100.00", EstadoPagamento.ESTORNADO));

        Conta conta = new Conta(new BigDecimal("380.00"), consumos, pagamentos);

        assertThat(conta.totalConsumos()).isEqualByComparingTo("10.00");
        assertThat(conta.total()).isEqualByComparingTo("390.00");     // 380 + 10 (cancelado sai)
        assertThat(conta.pago()).isEqualByComparingTo("400.00");      // estornado sai
        assertThat(conta.saldoDevedor()).isEqualByComparingTo("-10.00"); // crédito ao hóspede
        assertThat(conta.quitada()).isTrue();
    }
}
