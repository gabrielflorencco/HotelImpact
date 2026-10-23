package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Regras de negócio das hospedagens: a ocupação real do quarto, originada de
 * uma reserva (1:1). O check-in exige reserva ativa (pendente ou confirmada)
 * na data prevista, sem hospedagem anterior, e quarto ativo e disponível; o
 * check-out encerra a hospedagem, conclui a reserva e envia o quarto para
 * limpeza. O encerramento excepcional (com saldo pendente) fica para a fase de
 * pagamentos, quando o saldo puder ser calculado.
 *
 * As integridades são reforçadas no banco: 1:1 reserva–hospedagem (unique),
 * uma única hospedagem EM_ANDAMENTO por quarto (partial unique index) e
 * versionamento otimista.
 */
@Service
@RequiredArgsConstructor
public class HospedagemService {

    private final HospedagemRepository hospedagens;
    private final ReservaRepository reservas;
    private final QuartoRepository quartos;
    private final PagamentoService pagamentoService;

    @Transactional(readOnly = true)
    public Page<Hospedagem> buscar(EstadoHospedagem estado, Pageable pageable) {
        return estado == null
                ? hospedagens.buscarTodos(pageable)
                : hospedagens.buscarPorEstado(estado, pageable);
    }

    @Transactional(readOnly = true)
    public Hospedagem buscarPorId(Long id) {
        return hospedagens.findByIdComDetalhes(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Hospedagem não encontrada."));
    }

    /** Hospedagem vinculada a uma reserva, se existir (1:1). */
    @Transactional(readOnly = true)
    public Optional<Hospedagem> buscarPorReserva(Long reservaId) {
        return hospedagens.findByReservaId(reservaId);
    }

    /**
     * Check-in: cria a hospedagem a partir da reserva elegível e ocupa o quarto.
     * A reserva permanece em seu estado ativo (pendente ou confirmada); ela só
     * muda para CONCLUIDA no check-out.
     */
    @Transactional
    public Hospedagem checkIn(Long reservaId) {
        Reserva reserva = reservas.findByIdComDetalhes(reservaId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Reserva não encontrada."));
        validarElegibilidadeCheckIn(reserva);

        Hospedagem hospedagem = new Hospedagem();
        hospedagem.setReserva(reserva);
        hospedagem.setQuarto(reserva.getQuarto());
        hospedagem.setCheckInReal(LocalDateTime.now());
        hospedagem.setEstado(EstadoHospedagem.EM_ANDAMENTO);

        Quarto quarto = reserva.getQuarto();
        quarto.setEstado(EstadoQuarto.OCUPADO);
        quartos.save(quarto);

        return hospedagens.save(hospedagem);
    }

    /** Check-out com conta quitada: encerra normalmente, sem justificativa. */
    @Transactional
    public void checkOut(Long hospedagemId) {
        checkOut(hospedagemId, null);
    }

    /**
     * Check-out: conclui a reserva e envia o quarto para limpeza. Com a conta
     * quitada (reserva + consumos ativos − pagamentos confirmados), a
     * hospedagem é ENCERRADA normalmente. Com saldo pendente, exige
     * justificativa e caracteriza o encerramento excepcional: a hospedagem
     * fica CANCELADA com o motivo registrado.
     */
    @Transactional
    public void checkOut(Long hospedagemId, String motivoExcecao) {
        Hospedagem hospedagem = buscarPorId(hospedagemId);
        if (hospedagem.getEstado() != EstadoHospedagem.EM_ANDAMENTO) {
            throw new NegocioException("Somente hospedagens em andamento podem ser encerradas.");
        }

        java.math.BigDecimal saldo = pagamentoService.saldoDevedor(hospedagemId);
        if (saldo.signum() > 0) {
            if (motivoExcecao == null || motivoExcecao.isBlank()) {
                throw new NegocioException("A conta possui saldo pendente de R$ "
                        + saldo.toPlainString() + ". Informe a justificativa do encerramento excepcional.");
            }
            hospedagem.setMotivoExcecaoEncerramento(motivoExcecao.trim());
            hospedagem.setEstado(EstadoHospedagem.CANCELADA);
        } else {
            hospedagem.setEstado(EstadoHospedagem.ENCERRADA);
        }
        hospedagem.setCheckOutReal(LocalDateTime.now());

        Reserva reserva = hospedagem.getReserva();
        if (reserva.isAtiva()) {
            reserva.setEstado(EstadoReserva.CONCLUIDA);
            reservas.save(reserva);
        }

        Quarto quarto = hospedagem.getQuarto();
        quarto.setEstado(EstadoQuarto.LIMPEZA);
        quartos.save(quarto);
    }

    /**
     * Elegibilidade de check-in para exibição em telas: mesma regra do check-in,
     * sem duplicação de lógica — apenas traduz a validação em booleano.
     */
    @Transactional(readOnly = true)
    public boolean podeFazerCheckIn(Reserva reserva) {
        try {
            validarElegibilidadeCheckIn(reserva);
            return true;
        } catch (NegocioException ex) {
            return false;
        }
    }

    /** Reserva ativa, sem hospedagem, cuja data de check-in já chegou e quarto pronto. */
    private void validarElegibilidadeCheckIn(Reserva reserva) {
        if (!reserva.isAtiva()) {
            throw new NegocioException("Somente reservas pendentes ou confirmadas permitem check-in.");
        }
        if (hospedagens.existsByReservaId(reserva.getId())) {
            throw new NegocioException("A reserva já possui uma hospedagem.");
        }
        Quarto quarto = reserva.getQuarto();
        if (!Boolean.TRUE.equals(quarto.getAtivo())) {
            throw new NegocioException(
                    "O quarto " + quarto.getNumero() + " está desativado. Reative-o para recebê-la.");
        }
        switch (quarto.getEstado()) {
            case DISPONIVEL -> { /* quarto pronto para receber o hóspede */ }
            case LIMPEZA -> throw new NegocioException(
                    "O quarto " + quarto.getNumero() + " está aguardando limpeza.");
            case OCUPADO -> throw new NegocioException(
                    "O quarto " + quarto.getNumero() + " está ocupado por outra hospedagem.");
            default -> throw new NegocioException(
                    "O quarto " + quarto.getNumero() + " está fora de operação (" + quarto.getEstado()
                            + ") e não pode receber hospedagens.");
        }
        if (hospedagens.existsByQuartoIdAndEstado(quarto.getId(), EstadoHospedagem.EM_ANDAMENTO)) {
            throw new NegocioException(
                    "Não é possível fazer check-in: já existe uma hospedagem em andamento no quarto "
                            + quarto.getNumero() + ".");
        }
        if (LocalDate.now().isBefore(reserva.getDataCheckIn())) {
            throw new NegocioException("Check-in só é permitido a partir da data prevista na reserva ("
                    + reserva.getDataCheckIn() + ").");
        }
    }
}
