package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.ItemCatalogo;
import com.gabrielflorencco.hotelimpact.enums.TipoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ItemCatalogoRepository extends JpaRepository<ItemCatalogo, Long> {

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    boolean existsByNomeIgnoreCase(String nome);

    List<ItemCatalogo> findAllByOrderByNomeAsc();

    java.util.Optional<ItemCatalogo> findByNome(String nome);

    List<ItemCatalogo> findByTipoOrderByNomeAsc(TipoItem tipo);

    /** Produtos com controle de estoque no limite ou abaixo do mínimo. */
    List<ItemCatalogo> findByControlaEstoqueTrueAndTipoAndEstoqueAtualLessThanEqual(TipoItem tipo, Integer estoqueAtual);

    /** Itens disponíveis para lançamento em consumos, ordenados por nome. */
    List<ItemCatalogo> findAllByAtivoTrueOrderByNomeAsc();

    /** Produtos com controle de estoque no mínimo ou abaixo dele. */
    @Query("""
            SELECT count(i) FROM ItemCatalogo i
            WHERE i.controlaEstoque = true
              AND i.estoqueAtual <= i.estoqueMinimo
            """)
    long countComEstoqueBaixo();
}
