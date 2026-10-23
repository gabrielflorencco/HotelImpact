package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemCatalogoRepository extends JpaRepository<ItemCatalogo, Long> {

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    boolean existsByNomeIgnoreCase(String nome);

    List<ItemCatalogo> findAllByOrderByNomeAsc();

    List<ItemCatalogo> findByTipoOrderByNomeAsc(TipoItem tipo);

    /** Produtos com controle de estoque no limite ou abaixo do mínimo. */
    List<ItemCatalogo> findByControlaEstoqueTrueAndTipoAndEstoqueAtualLessThanEqual(TipoItem tipo, Integer estoqueAtual);

    /** Itens disponíveis para lançamento em consumos, ordenados por nome. */
    List<ItemCatalogo> findAllByAtivoTrueOrderByNomeAsc();
}
