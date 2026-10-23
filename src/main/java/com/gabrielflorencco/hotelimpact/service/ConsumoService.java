package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.dto.ConsumoForm;
import com.gabrielflorencco.hotelimpact.entity.Consumo;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.enums.EstadoConsumo;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.ConsumoRepository;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Regras de negócio dos consumos lançados numa hospedagem: preço unitário
 * histórico capturado no lançamento, total calculado no servidor, baixa de
 * estoque apenas para produtos com controle efetivo e cancelamento como
 * estorno lógico que restitui o estoque uma única vez. Lançamentos nunca
 * são excluídos — preservam o histórico da conta.
 */
@Service
@RequiredArgsConstructor
public class ConsumoService {

    private final ConsumoRepository consumos;
    private final HospedagemRepository hospedagens;
    private final ItemCatalogoRepository itens;

    /** Lançamentos da conta da hospedagem (com item buscado, anti-N+1), mais recentes primeiro. */
    @Transactional(readOnly = true)
    public List<Consumo> listarPorHospedagem(Long hospedagemId) {
        return consumos.findByHospedagemIdComItem(hospedagemId);
    }

    /** Soma dos lançamentos ativos: cancelados não entram na conta. */
    @Transactional(readOnly = true)
    public BigDecimal totalAtivos(Long hospedagemId) {
        return consumos.findByHospedagemIdAndEstado(hospedagemId, EstadoConsumo.ATIVO).stream()
                .map(Consumo::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Lança um consumo na hospedagem em andamento, capturando o preço histórico e baixando o estoque. */
    @Transactional
    public Consumo lancar(Long hospedagemId, ConsumoForm form) {
        Hospedagem hospedagem = hospedagens.findByIdComDetalhes(hospedagemId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Hospedagem não encontrada."));
        if (hospedagem.getEstado() != EstadoHospedagem.EM_ANDAMENTO) {
            throw new NegocioException("Consumos só podem ser lançados em hospedagens em andamento.");
        }

        ItemCatalogo item = itens.findById(form.getItemId())
                .orElseThrow(() -> new RegistroNaoEncontradoException("Item do catálogo não encontrado."));
        if (!Boolean.TRUE.equals(item.getAtivo())) {
            throw new NegocioException("O item \"" + item.getNome() + "\" está inativo e não pode ser lançado.");
        }

        Consumo consumo = new Consumo();
        consumo.setHospedagem(hospedagem);
        consumo.setItem(item);
        consumo.setQuantidade(form.getQuantidade());
        consumo.setPrecoUnitario(item.getPrecoUnitario());
        consumo.setValorTotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(form.getQuantidade())));
        consumo.setEstado(EstadoConsumo.ATIVO);
        consumo.setObservacoes(nuloSeVazio(form.getObservacoes()));

        if (item.isEstoqueControladoEfetivo()) {
            if (item.getEstoqueAtual() < form.getQuantidade()) {
                throw new NegocioException("Estoque insuficiente de \"" + item.getNome() + "\": restam "
                        + item.getEstoqueAtual() + " unidade(s).");
            }
            item.setEstoqueAtual(item.getEstoqueAtual() - form.getQuantidade());
            itens.save(item);
        }
        return consumos.save(consumo);
    }

    /**
     * Cancelamento (estorno lógico): o lançamento é preservado e o estoque é
     * restituído uma única vez — um segundo cancelamento é rejeitado.
     */
    @Transactional
    public void cancelar(Long hospedagemId, Long consumoId) {
        Hospedagem hospedagem = hospedagens.findById(hospedagemId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Hospedagem não encontrada."));
        if (hospedagem.getEstado() != EstadoHospedagem.EM_ANDAMENTO) {
            throw new NegocioException("Consumos só podem ser cancelados em hospedagens em andamento.");
        }

        Consumo consumo = consumos.findByIdAndHospedagemId(consumoId, hospedagemId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Consumo não encontrado nesta hospedagem."));
        if (consumo.getEstado() == EstadoConsumo.CANCELADO) {
            throw new NegocioException("O lançamento já está cancelado.");
        }

        consumo.setEstado(EstadoConsumo.CANCELADO);
        consumos.save(consumo);

        ItemCatalogo item = consumo.getItem();
        if (item.isEstoqueControladoEfetivo()) {
            item.setEstoqueAtual(item.getEstoqueAtual() + consumo.getQuantidade());
            itens.save(item);
        }
    }

    private String nuloSeVazio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
