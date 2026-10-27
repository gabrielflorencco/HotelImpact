package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.dto.OrdemManutencaoForm;
import com.gabrielflorencco.hotelimpact.entity.OrdemManutencao;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoManutencao;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.PrioridadeManutencao;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.OrdemManutencaoRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras de negócio das ordens de manutenção: uma ordem ativa por quarto,
 * abertura proibida com hospedagem em andamento (o hóspede não pode ser
 * despejado por manutenção) e prioridade CRÍTICA interdita o quarto — a não
 * ser que ele já esteja interditado por outra razão, caso em que a ordem
 * não assume a interdição e não vai liberá-lo ao final.
 *
 * A execução coloca o quarto em MANUTENCAO; a conclusão ou o cancelamento
 * devolvem DISPONIVEL apenas quando o estado foi causado pela própria
 * ordem — interdições administrativas prevalecem.
 */
@Service
@RequiredArgsConstructor
public class ManutencaoService {

    private static final List<EstadoManutencao> ESTADOS_ATIVOS =
            List.of(EstadoManutencao.ABERTA, EstadoManutencao.EM_ANDAMENTO);

    private final OrdemManutencaoRepository ordens;
    private final QuartoRepository quartos;
    private final HospedagemRepository hospedagens;

    @Transactional(readOnly = true)
    public Page<OrdemManutencao> buscar(EstadoManutencao estado, Pageable pageable) {
        return estado == null
                ? ordens.buscarTodos(pageable)
                : ordens.buscarPorEstado(estado, pageable);
    }

    @Transactional(readOnly = true)
    public OrdemManutencao buscarPorId(Long id) {
        return ordens.findByIdComQuarto(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Ordem de manutenção não encontrada."));
    }

    /** Abre a ordem; prioridade CRÍTICA interdita o quarto, se ainda não interditado. */
    @Transactional
    public OrdemManutencao abrir(OrdemManutencaoForm form) {
        Quarto quarto = quartos.findById(form.getQuartoId())
                .orElseThrow(() -> new RegistroNaoEncontradoException("Quarto não encontrado."));

        if (hospedagens.existsByQuartoIdAndEstado(quarto.getId(), EstadoHospedagem.EM_ANDAMENTO)) {
            throw new NegocioException(
                    "Não é possível abrir a manutenção: existe uma hospedagem em andamento no quarto "
                            + quarto.getNumero() + ".");
        }
        if (ordens.existsByQuartoIdAndEstadoIn(quarto.getId(), ESTADOS_ATIVOS)) {
            throw new NegocioException(
                    "Já existe uma manutenção ativa para o quarto " + quarto.getNumero() + ".");
        }

        OrdemManutencao ordem = new OrdemManutencao();
        ordem.setAbertaEm(java.time.LocalDateTime.now());
        ordem.setQuarto(quarto);
        ordem.setTitulo(form.getTitulo().trim());
        ordem.setDescricao(nuloSeVazio(form.getDescricao()));
        ordem.setPrioridade(form.getPrioridade());
        ordem.setObservacoes(nuloSeVazio(form.getObservacoes()));
        ordem.setEstado(EstadoManutencao.ABERTA);

        boolean criticaInterditando = form.getPrioridade() == PrioridadeManutencao.CRITICA
                && quarto.getEstado() != EstadoQuarto.INTERDITADO;
        ordem.setInterditouQuarto(criticaInterditando);
        if (criticaInterditando) {
            quarto.interditar();
            quartos.save(quarto);
        }
        return ordens.save(ordem);
    }

    /** Inicia a execução: o quarto em operação passa a MANUTENCAO. */
    @Transactional
    public void iniciar(Long id) {
        OrdemManutencao ordem = buscarPorId(id);
        ordem.iniciarExecucao();

        Quarto quarto = ordem.getQuarto();
        if (quarto.getEstado() == EstadoQuarto.DISPONIVEL || quarto.getEstado() == EstadoQuarto.LIMPEZA) {
            quarto.iniciarManutencao();
            quartos.save(quarto);
        }
    }

    /** Conclui a ordem e devolve o quarto a DISPONIVEL quando o estado foi causado por ela. */
    @Transactional
    public void concluir(Long id) {
        OrdemManutencao ordem = buscarPorId(id);
        ordem.concluirOrdem();
        liberarQuartoSeCausadoPelaOrdem(ordem);
    }

    /** Cancela a ordem (remoção lógica) com a mesma restauração do quarto. */
    @Transactional
    public void cancelar(Long id) {
        OrdemManutencao ordem = buscarPorId(id);
        ordem.cancelarOrdem();
        liberarQuartoSeCausadoPelaOrdem(ordem);
    }

    /**
     * Restaura o quarto apenas quando a ordem é responsável pelo estado atual:
     * interdição da própria ordem (CRÍTICA) ou estado MANUTENCAO posto pela
     * execução. Interdição administrativa (QuartoService) prevalece.
     */
    private void liberarQuartoSeCausadoPelaOrdem(OrdemManutencao ordem) {
        Quarto quarto = ordem.getQuarto();
        if (Boolean.TRUE.equals(ordem.getInterditouQuarto())
                || quarto.getEstado() == EstadoQuarto.MANUTENCAO) {
            quarto.disponibilizar();
            quartos.save(quarto);
        }
    }

    private String nuloSeVazio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
