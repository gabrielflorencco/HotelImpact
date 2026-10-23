package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.dto.PagamentoForm;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.Pagamento;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoConsumo;
import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.ConsumoRepository;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Regras de negócio dos pagamentos de uma hospedagem: registro confirmado,
 * estorno lógico único (o valor original nunca é editado) e o saldo devedor
 * da conta — reserva + consumos ativos − pagamentos confirmados. Estornados
 * não contam como recebidos.
 */
@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentos;
    private final HospedagemRepository hospedagens;
    private final ConsumoRepository consumos;

    /** Pagamentos da conta, do mais antigo ao mais recente. */
    @Transactional(readOnly = true)
    public List<Pagamento> listarPorHospedagem(Long hospedagemId) {
        return pagamentos.findByHospedagemIdOrderByDataHoraAsc(hospedagemId);
    }

    /** Soma dos pagamentos confirmados (estornados não contam como recebidos). */
    @Transactional(readOnly = true)
    public BigDecimal totalConfirmados(Long hospedagemId) {
        return pagamentos.findByHospedagemIdAndEstado(hospedagemId, EstadoPagamento.CONFIRMADO).stream()
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total da conta: reserva + consumos ativos (cancelados não entram). */
    @Transactional(readOnly = true)
    public BigDecimal totalConta(Long hospedagemId) {
        Hospedagem hospedagem = hospedagens.findByIdComDetalhes(hospedagemId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Hospedagem não encontrada."));
        BigDecimal totalConsumos = consumos.findByHospedagemIdAndEstado(hospedagemId, EstadoConsumo.ATIVO).stream()
                .map(c -> c.getValorTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return hospedagem.getReserva().getValorTotal().add(totalConsumos);
    }

    /** Saldo devedor: conta − pagamentos confirmados. Negativo significa crédito. */
    @Transactional(readOnly = true)
    public BigDecimal saldoDevedor(Long hospedagemId) {
        return totalConta(hospedagemId).subtract(totalConfirmados(hospedagemId));
    }

    /** Registra um pagamento confirmado na conta da hospedagem. */
    @Transactional
    public Pagamento registrar(Long hospedagemId, PagamentoForm form) {
        Hospedagem hospedagem = hospedagens.findById(hospedagemId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Hospedagem não encontrada."));
        if (hospedagem.getEstado() == EstadoHospedagem.CANCELADA) {
            throw new NegocioException(
                    "A hospedagem foi encerrada excepcionalmente e não aceita novos pagamentos.");
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setHospedagem(hospedagem);
        pagamento.setValor(form.getValor());
        pagamento.setForma(form.getForma());
        pagamento.setEstado(EstadoPagamento.CONFIRMADO);
        pagamento.setObservacao(nuloSeVazio(form.getObservacao()));
        return pagamentos.save(pagamento);
    }

    /**
     * Estorno lógico, uma única vez: o pagamento original permanece no
     * histórico e o valor confirmado deixa de contar como recebido.
     */
    @Transactional
    public void estornar(Long hospedagemId, Long pagamentoId) {
        Pagamento pagamento = pagamentos.findByIdAndHospedagemId(pagamentoId, hospedagemId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Pagamento não encontrado nesta hospedagem."));
        if (pagamento.getEstado() == EstadoPagamento.ESTORNADO) {
            throw new NegocioException("O pagamento já está estornado.");
        }
        pagamento.setEstado(EstadoPagamento.ESTORNADO);
        pagamentos.save(pagamento);
    }

    private String nuloSeVazio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
