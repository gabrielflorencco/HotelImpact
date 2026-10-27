package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.dto.ReservaForm;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.HospedeRepository;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Regras de negócio das reservas: períodos semiabertos [check-in, check-out),
 * disponibilidade do quarto (estado operacional + reservas ativas conflitantes),
 * capacidade efetiva, diária histórica com total calculado no servidor,
 * exceção administrativa para datas passadas e código único gerado pelo sistema.
 *
 * Reservas nunca são excluídas fisicamente: preservam o histórico (mesma regra
 * que bloqueia a exclusão de hóspedes e quartos vinculados); o cancelamento é
 * a única forma de liberar o período.
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final List<EstadoReserva> ESTADOS_ATIVOS =
            List.of(EstadoReserva.PENDENTE, EstadoReserva.CONFIRMADA);

    /** Estados que tiram o quarto de operação e impedem novas reservas. */
    private static final Set<EstadoQuarto> ESTADOS_FORA_DE_OPERACAO =
            Set.of(EstadoQuarto.INTERDITADO, EstadoQuarto.MANUTENCAO);

    private final ReservaRepository reservas;
    private final HospedeRepository hospedes;
    private final QuartoRepository quartos;
    private final HospedagemRepository hospedagens;

    @Transactional(readOnly = true)
    public Page<Reserva> buscar(EstadoReserva estado, Pageable pageable) {
        return estado == null
                ? reservas.buscarTodos(pageable)
                : reservas.buscarPorEstado(estado, pageable);
    }

    @Transactional(readOnly = true)
    public Reserva buscarPorId(Long id) {
        return reservas.findByIdComDetalhes(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Reserva não encontrada."));
    }

    @Transactional
    public Reserva criar(ReservaForm form) {
        validarPeriodo(form.getDataCheckIn(), form.getDataCheckOut());

        Hospede hospede = hospedes.findById(form.getHospedeId())
                .orElseThrow(() -> new RegistroNaoEncontradoException("Hóspede não encontrado."));
        Quarto quarto = quartos.findByIdComCategoria(form.getQuartoId())
                .orElseThrow(() -> new RegistroNaoEncontradoException("Quarto não encontrado."));

        exigirQuartoReservavel(quarto);
        validarCapacidade(quarto, form.getQuantidadeHospedes());
        exigirPeriodoLivre(quarto, form.getDataCheckIn(), form.getDataCheckOut());
        validarExcecaoDeDataPassada(form);

        Reserva reserva = new Reserva();
        reserva.setCodigo(gerarCodigo());
        reserva.setHospede(hospede);
        reserva.setQuarto(quarto);
        reserva.setDataCheckIn(form.getDataCheckIn());
        reserva.setDataCheckOut(form.getDataCheckOut());
        reserva.setQuantidadeHospedes(form.getQuantidadeHospedes());
        reserva.setValorDiaria(form.getValorDiaria());

        BigDecimal desconto = form.getDesconto() == null ? BigDecimal.ZERO : form.getDesconto();
        validarDesconto(reserva, desconto);
        reserva.setDesconto(desconto);
        reserva.setValorTotal(reserva.getValorBruto().subtract(desconto));

        reserva.setMotivoExcecao(nuloSeVazio(form.getMotivoExcecao()));
        reserva.setObservacoes(nuloSeVazio(form.getObservacoes()));
        return reservas.save(reserva);
    }

    @Transactional
    public void confirmar(Long id) {
        Reserva reserva = buscarPorId(id);
        reserva.confirmar();
        reservas.save(reserva);
    }

    @Transactional
    public void cancelar(Long id) {
        Reserva reserva = buscarPorId(id);
        if (!reserva.isAtiva()) {
            throw new NegocioException("Somente reservas pendentes ou confirmadas podem ser canceladas.");
        }
        if (hospedagens.existsByReservaId(id)) {
            throw new NegocioException("Não é possível cancelar a reserva: o check-in já foi realizado.");
        }
        reserva.cancelar();
        reservas.save(reserva);
    }

    /**
     * No-show: registra que o hóspede não apareceu. A reserva ativa cuja data
     * de check-in já passou (e sem hospedagem) deixa de bloquear o período.
     */
    @Transactional
    public void registrarNoShow(Long id) {
        Reserva reserva = buscarPorId(id);
        if (!podeRegistrarNoShow(reserva)) {
            if (hospedagens.existsByReservaId(id)) {
                throw new NegocioException("Não é possível registrar no-show: a reserva possui check-in.");
            }
            if (reserva.isAtiva()) {
                throw new NegocioException(
                        "No-show só pode ser registrado após a data prevista de check-in (" + reserva.getDataCheckIn() + ").");
            }
            throw new NegocioException("Somente reservas pendentes ou confirmadas podem ser registradas como no-show.");
        }
        reserva.registrarNoShow();
        reservas.save(reserva);
    }

    /** Elegibilidade de no-show para exibição em telas: ativa, sem check-in e data já passada. */
    @Transactional(readOnly = true)
    public boolean podeRegistrarNoShow(Reserva reserva) {
        return reserva.isAtiva()
                && LocalDate.now().isAfter(reserva.getDataCheckIn())
                && !hospedagens.existsByReservaId(reserva.getId());
    }

    /** Períodos são semiabertos: o check-out deve ser posterior ao check-in. */
    private void validarPeriodo(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new NegocioException("A data de check-out deve ser posterior à data de check-in.");
        }
    }

    /** O quarto precisa estar ativo e em operação para receber reservas. */
    private void exigirQuartoReservavel(Quarto quarto) {
        if (!Boolean.TRUE.equals(quarto.getAtivo())) {
            throw new NegocioException(
                    "O quarto " + quarto.getNumero() + " está desativado. Reative-o para utilizá-lo em reservas.");
        }
        if (ESTADOS_FORA_DE_OPERACAO.contains(quarto.getEstado())) {
            throw new NegocioException(
                    "O quarto " + quarto.getNumero() + " está fora de operação (" + quarto.getEstado()
                            + ") e não pode receber reservas.");
        }
    }

    /** A capacidade efetiva considera a sobreposição opcional do quarto sobre a categoria. */
    private void validarCapacidade(Quarto quarto, Integer quantidadeHospedes) {
        int capacidadeEfetiva = quarto.getCapacidadeEfetiva();
        if (quantidadeHospedes > capacidadeEfetiva) {
            throw new NegocioException("Capacidade excedida: o quarto " + quarto.getNumero()
                    + " comporta " + capacidadeEfetiva + " hóspede(s) e foram informados "
                    + quantidadeHospedes + ".");
        }
    }

    /** Reservas ativas (pendentes ou confirmadas) conflitantes bloqueiam o período. */
    private void exigirPeriodoLivre(Quarto quarto, LocalDate checkIn, LocalDate checkOut) {
        if (!reservas.findConflitantes(quarto.getId(), checkIn, checkOut, ESTADOS_ATIVOS).isEmpty()) {
            throw new NegocioException("Já existe uma reserva ativa para o quarto " + quarto.getNumero()
                    + " no período informado.");
        }
    }

    /** Exceção administrativa: reservas para datas passadas exigem justificativa. */
    private void validarExcecaoDeDataPassada(ReservaForm form) {
        if (form.getDataCheckIn().isBefore(LocalDate.now())
                && (form.getMotivoExcecao() == null || form.getMotivoExcecao().isBlank())) {
            throw new NegocioException(
                    "Reservas para datas passadas exigem uma justificativa de exceção.");
        }
    }

    /** O desconto nunca pode exceder o valor bruto (noites × diária). */
    private void validarDesconto(Reserva reserva, BigDecimal desconto) {
        if (desconto.compareTo(reserva.getValorBruto()) > 0) {
            throw new NegocioException("O desconto não pode exceder o valor bruto da reserva (R$ "
                    + reserva.getValorBruto().toPlainString() + ").");
        }
    }

    /**
     * Código legível no formato RES-ANO-NNNNNN (ex.: RES-2026-000123). A
     * sequência parte da contagem de códigos do ano e avança enquanto houver
     * colisão com registros existentes (códigos de testes, por exemplo). A
     * constraint de unicidade do banco é a última linha de defesa contra a
     * corrida entre duas criações simultâneas.
     */
    private String gerarCodigo() {
        String prefixo = "RES-" + LocalDate.now().getYear() + "-";
        long sequencia = reservas.countByCodigoStartingWith(prefixo);
        for (long candidato = sequencia + 1; ; candidato++) {
            String codigo = prefixo + "%06d".formatted(candidato);
            if (!reservas.existsByCodigo(codigo)) {
                return codigo;
            }
        }
    }

    private String nuloSeVazio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
