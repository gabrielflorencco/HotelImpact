package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.dto.ItemCatalogoForm;
import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.ConsumoRepository;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de negócio do catálogo de produtos e serviços: nome único, preço
 * positivo, serviços nunca controlam estoque (a flag é ignorada) e
 * exclusão bloqueada quando há lançamentos de consumo que preservam o
 * histórico — nesses casos, orienta-se a desativação.
 */
@Service
@RequiredArgsConstructor
public class ItemCatalogoService {

    private final ItemCatalogoRepository itens;
    private final ConsumoRepository consumos;

    @Transactional(readOnly = true)
    public List<ItemCatalogo> listar() {
        return itens.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public ItemCatalogo buscarPorId(Long id) {
        return itens.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Item não encontrado."));
    }

    @Transactional
    public ItemCatalogo salvar(ItemCatalogoForm form) {
        validarNomeUnico(form.getNome().trim(), form.getId());

        ItemCatalogo item = form.getId() == null
                ? new ItemCatalogo()
                : buscarPorId(form.getId());
        item.setNome(form.getNome().trim());
        item.setDescricao(form.getDescricao() == null ? null : form.getDescricao().trim());
        item.setTipo(form.getTipo());
        item.setPrecoUnitario(form.getPrecoUnitario());
        boolean controlaEstoque = TipoItem.PRODUTO == form.getTipo()
                && Boolean.TRUE.equals(form.getControlaEstoque());
        item.setControlaEstoque(controlaEstoque);
        item.setEstoqueAtual(controlaEstoque ? form.getEstoqueAtual() : 0);
        item.setEstoqueMinimo(controlaEstoque ? form.getEstoqueMinimo() : 0);
        item.setAtivo(form.getAtivo() != null && form.getAtivo());
        return itens.save(item);
    }

    /**
     * Exclusão física permitida apenas para itens sem lançamentos; consumos
     * preservam o histórico e referenciam o item, então nesses casos
     * orienta-se a desativação.
     */
    @Transactional
    public void excluir(Long id) {
        ItemCatalogo item = buscarPorId(id);
        if (consumos.existsByItemId(id)) {
            throw new NegocioException(
                    "Não é possível excluir o item: existem consumos registrados que preservam o histórico. Desative-o.");
        }
        itens.delete(item);
    }

    private void validarNomeUnico(String nome, Long id) {
        boolean duplicado = id == null
                ? itens.existsByNomeIgnoreCase(nome)
                : itens.existsByNomeIgnoreCaseAndIdNot(nome, id);
        if (duplicado) {
            throw new NegocioException("Já existe um item com o nome informado.");
        }
    }
}
