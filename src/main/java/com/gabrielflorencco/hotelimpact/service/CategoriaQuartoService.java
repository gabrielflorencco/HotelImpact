package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.exception.RegistroNaoEncontradoException;
import com.gabrielflorencco.hotelimpact.repository.CategoriaQuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de negócio das categorias de quarto: nome único, exclusão bloqueada
 * quando há quartos referenciando e controle de ativação.
 */
@Service
@RequiredArgsConstructor
public class CategoriaQuartoService {

    private final CategoriaQuartoRepository categorias;
    private final QuartoRepository quartos;

    @Transactional(readOnly = true)
    public List<CategoriaQuarto> listar() {
        return categorias.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public CategoriaQuarto buscarPorId(Long id) {
        return categorias.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Categoria não encontrada."));
    }

    @Transactional
    public CategoriaQuarto salvar(CategoriaForm form) {
        validarNomeUnico(form.getNome(), form.getId());

        CategoriaQuarto categoria = form.getId() == null
                ? new CategoriaQuarto()
                : buscarPorId(form.getId());
        categoria.setNome(form.getNome().trim());
        categoria.setDescricao(form.getDescricao() == null ? null : form.getDescricao().trim());
        categoria.setCapacidadeMaxima(form.getCapacidadeMaxima());
        categoria.setQuantidadeCamas(form.getQuantidadeCamas());
        categoria.setPrecoBase(form.getPrecoBase());
        categoria.setAtivo(form.getAtivo() != null && form.getAtivo());
        return categorias.save(categoria);
    }

    /**
     * Exclusão física permitida apenas quando nenhum quarto referencia a categoria;
     * caso contrário, orienta-se a desativação, preservando o histórico.
     */
    @Transactional
    public void excluir(Long id) {
        CategoriaQuarto categoria = buscarPorId(id);
        long quartosVinculados = quartos.countByCategoriaId(id);
        if (quartosVinculados > 0) {
            throw new NegocioException(
                    "Não é possível excluir a categoria: existem %d quarto(s) utilizando-a. Desative-a para impedir novos vínculos."
                            .formatted(quartosVinculados));
        }
        categorias.delete(categoria);
    }

    private void validarNomeUnico(String nome, Long id) {
        boolean duplicado = id == null
                ? categorias.existsByNomeIgnoreCase(nome.trim())
                : categorias.existsByNomeIgnoreCaseAndIdNot(nome.trim(), id);
        if (duplicado) {
            throw new NegocioException("Já existe uma categoria com o nome informado.");
        }
    }
}
