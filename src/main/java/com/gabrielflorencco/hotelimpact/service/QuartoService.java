package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.CategoriaQuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.OrdemManutencaoRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import com.gabrielflorencco.hotelimpact.repository.TarefaLimpezaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de negócio dos quartos: número único, categoria ativa para novos
 * quartos, estados alterados somente por operações explícitas e exclusão
 * bloqueada quando há histórico.
 */
@Service
@RequiredArgsConstructor
public class QuartoService {

    private final QuartoRepository quartos;
    private final CategoriaQuartoRepository categorias;
    private final ReservaRepository reservas;
    private final HospedagemRepository hospedagens;
    private final TarefaLimpezaRepository tarefasLimpeza;
    private final OrdemManutencaoRepository ordensManutencao;
    private final com.gabrielflorencco.hotelimpact.service.AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<Quarto> buscar(String numero, EstadoQuarto estado, Long categoriaId) {
        return quartos.buscar(vazioParaNulo(numero), estado, categoriaId);
    }

    @Transactional(readOnly = true)
    public Quarto buscarPorId(Long id) {
        return quartos.findByIdComCategoria(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Quarto não encontrado."));
    }

    @Transactional
    public Quarto salvar(QuartoForm form) {
        validarNumeroUnico(form.getNumero().trim(), form.getId());
        CategoriaQuarto categoria = categorias.findById(form.getCategoriaId())
                .orElseThrow(() -> new NegocioException("Categoria informada não existe."));

        Quarto quarto;
        if (form.getId() == null) {
            exigirCategoriaAtiva(categoria);
            quarto = new Quarto();
            quarto.setEstado(EstadoQuarto.DISPONIVEL);
        } else {
            quarto = buscarPorId(form.getId());
            if (!quarto.getCategoria().getId().equals(categoria.getId())) {
                exigirCategoriaAtiva(categoria);
            }
            exigirSemHospedagemAtiva(quarto, "alterar o quarto");
        }

        quarto.setNumero(form.getNumero().trim());
        quarto.setCategoria(categoria);
        quarto.setAndar(form.getAndar());
        quarto.setCapacidadeOperacional(form.getCapacidadeOperacional());
        quarto.setObservacoes(form.getObservacoes() == null ? null : form.getObservacoes().trim());
        quarto.setAtivo(form.getAtivo() != null && form.getAtivo());
        return quartos.save(quarto);
    }

    /**
     * Exclusão física permitida apenas para quartos sem qualquer histórico
     * (reservas, hospedagens, limpezas ou manutenções); caso contrário,
     * orienta-se a desativação.
     */
    @Transactional
    public void excluir(Long id) {
        Quarto quarto = buscarPorId(id);
        if (reservas.existsByQuartoId(id) || hospedagens.existsByQuartoId(id)
                || tarefasLimpeza.existsByQuartoId(id) || ordensManutencao.existsByQuartoId(id)) {
            throw new NegocioException(
                    "Não é possível excluir o quarto: ele possui histórico de reservas, hospedagens, limpeza ou manutenção. Desative-o.");
        }
        exigirSemHospedagemAtiva(quarto, "excluir o quarto");
        quartos.delete(quarto);
    }

    /** @return o estado final do quarto após a operação (true = ativo). */
    @Transactional
    public boolean alternarAtivo(Long id) {
        Quarto quarto = buscarPorId(id);
        if (Boolean.TRUE.equals(quarto.getAtivo())) {
            exigirSemHospedagemAtiva(quarto, "desativar o quarto");
            quarto.setAtivo(false);
        } else {
            quarto.setAtivo(true);
        }
        quartos.save(quarto);
        return quarto.getAtivo();
    }

    /** Interdição administrativa: quarto sai de operação e não pode receber hospedagens. */
    @Transactional
    public void interditar(Long id) {
        Quarto quarto = buscarPorId(id);
        exigirSemHospedagemAtiva(quarto, "interditar o quarto");
        quarto.interditar();
        quartos.save(quarto);
    }

    @Transactional
    public void liberar(Long id) {
        Quarto quarto = buscarPorId(id);
        quarto.liberarInterdicao();
        quartos.save(quarto);
    }

    /** Conclui a limpeza pós-check-out: o quarto volta a ficar disponível. */
    @Transactional
    public void concluirLimpeza(Long id) {
        Quarto quarto = buscarPorId(id);
        // A tarefa aberta do quarto (se houver) é concluída na mesma
        // transação — o histórico de limpeza preserva início e fim.
        tarefasLimpeza.findFirstByQuartoIdAndEstadoNotOrderByCriadaEmDesc(
                        id, com.gabrielflorencco.hotelimpact.enums.EstadoTarefaLimpeza.CONCLUIDA)
                .ifPresent(tarefa -> {
                    tarefa.concluir();
                    tarefasLimpeza.save(tarefa);
                });
        quarto.concluirLimpeza();
        quartos.save(quarto);
        auditoria.registrar(com.gabrielflorencco.hotelimpact.enums.OperacaoAuditoria.LIMPEZA_CONCLUIDA,
                "Quarto", quarto.getId(), "Quarto " + quarto.getNumero());
    }

    /** Inicia a execução da tarefa de limpeza aberta do quarto (governança em ação). */
    @Transactional
    public void iniciarLimpeza(Long id) {
        Quarto quarto = buscarPorId(id);
        if (quarto.getEstado() != EstadoQuarto.LIMPEZA) {
            throw new NegocioException("Somente quartos aguardando limpeza têm tarefa de limpeza.");
        }
        com.gabrielflorencco.hotelimpact.entity.TarefaLimpeza tarefa =
                tarefasLimpeza.findFirstByQuartoIdAndEstadoNotOrderByCriadaEmDesc(
                                id, com.gabrielflorencco.hotelimpact.enums.EstadoTarefaLimpeza.CONCLUIDA)
                        .orElseThrow(() -> new NegocioException(
                                "Não há tarefa de limpeza aberta para o quarto " + quarto.getNumero() + "."));
        tarefa.iniciar();
        tarefasLimpeza.save(tarefa);
        auditoria.registrar(com.gabrielflorencco.hotelimpact.enums.OperacaoAuditoria.LIMPEZA_INICIADA,
                "TarefaLimpeza", tarefa.getId(), "Quarto " + quarto.getNumero());
    }

    private void validarNumeroUnico(String numero, Long id) {
        boolean duplicado = id == null
                ? quartos.existsByNumero(numero)
                : quartos.existsByNumeroAndIdNot(numero, id);
        if (duplicado) {
            throw new NegocioException("Já existe um quarto com o número informado.");
        }
    }

    private void exigirCategoriaAtiva(CategoriaQuarto categoria) {
        if (!Boolean.TRUE.equals(categoria.getAtivo())) {
            throw new NegocioException(
                    "A categoria '" + categoria.getNome() + "' está inativa. Reative-a para utilizá-la em quartos.");
        }
    }

    private void exigirSemHospedagemAtiva(Quarto quarto, String operacao) {
        if (hospedagens.existsByQuartoIdAndEstado(quarto.getId(), EstadoHospedagem.EM_ANDAMENTO)) {
            throw new NegocioException("Não é possível " + operacao + ": existe uma hospedagem em andamento nele.");
        }
    }

    private String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
