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
    private final com.gabrielflorencco.hotelimpact.service.AuditoriaService auditoria;

    /** Pagamentos da conta, do mais antigo ao mais recente. */
    @Transactional(readOnly = true)
    public List<Pagamento> listarPorHospedagem(Long hospedagemId) {
        return pagamentos.findByHospedagemIdOrderByDataHoraAsc(hospedagemId);
    }

    /** Soma dos pagamentos confirmados (estornados não contam como recebidos). */
    @Transactional(readOnly = true)
    public BigDecimal totalConfirmados(Long hospedagemId) {
        return contaDe(hospedagemId).pago();
    }

    /**
     * Fechamento completo da conta da hospedagem como objeto de valor do
     * domínio ({@link com.gabrielflorencco.hotelimpact.domain.Conta}):
     * concentra total, pago e saldo com as fórmulas do negócio.
     */
    @Transactional(readOnly = true)
    public com.gabrielflorencco.hotelimpact.domain.Conta contaDe(Long hospedagemId) {
        Hospedagem hospedagem = hospedagens.findByIdComDetalhes(hospedagemId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Hospedagem não encontrada."));
        return new com.gabrielflorencco.hotelimpact.domain.Conta(
                hospedagem.getReserva().getValorTotal(),
                consumos.findByHospedagemIdComItem(hospedagemId),
                pagamentos.findByHospedagemIdOrderByDataHoraAsc(hospedagemId));
    }

    /** Total da conta: reserva + consumos ativos (cancelados não entram). */
    @Transactional(readOnly = true)
    public BigDecimal totalConta(Long hospedagemId) {
        return contaDe(hospedagemId).total();
    }

    /** Saldo devedor: conta − pagamentos confirmados. Negativo significa crédito. */
    @Transactional(readOnly = true)
    public BigDecimal saldoDevedor(Long hospedagemId) {
        return contaDe(hospedagemId).saldoDevedor();
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
        pagamento.setDataHora(java.time.LocalDateTime.now());
        pagamento.setValor(form.getValor());
        pagamento.setForma(form.getForma());
        pagamento.setEstado(EstadoPagamento.CONFIRMADO);
        pagamento.setObservacao(nuloSeVazio(form.getObservacao()));
        Pagamento salvo = pagamentos.save(pagamento);
        auditoria.registrar(com.gabrielflorencco.hotelimpact.enums.OperacaoAuditoria.PAGAMENTO_REGISTRADO,
                "Pagamento", salvo.getId(),
                "Hospedagem " + hospedagemId + " · " + form.getForma());
        return salvo;
    }

    /**
     * Estorno lógico, uma única vez: o pagamento original permanece no
     * histórico e o valor confirmado deixa de contar como recebido.
     */
    @Transactional
    public void estornar(Long hospedagemId, Long pagamentoId) {
        Pagamento pagamento = pagamentos.findByIdAndHospedagemId(pagamentoId, hospedagemId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Pagamento não encontrado nesta hospedagem."));
        pagamento.estornar();
        pagamentos.save(pagamento);
        auditoria.registrar(com.gabrielflorencco.hotelimpact.enums.OperacaoAuditoria.PAGAMENTO_ESTORNADO,
                "Pagamento", pagamentoId, "Hospedagem " + hospedagemId);
    }

    private String nuloSeVazio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
