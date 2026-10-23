package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.OrdemManutencao;
import com.gabrielflorencco.hotelimpact.enums.EstadoManutencao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdemManutencaoRepository extends JpaRepository<OrdemManutencao, Long> {

    boolean existsByQuartoId(Long quartoId);

    /** Uma manutenção por vez: guarda contra ordem ativa no mesmo quarto. */
    boolean existsByQuartoIdAndEstadoIn(Long quartoId, List<EstadoManutencao> estados);

    /** Detalhe com quarto já buscado (a view acessa o número fora da transação). */
    @Query("""
            SELECT o FROM OrdemManutencao o
            JOIN FETCH o.quarto
            WHERE o.id = :id
            """)
    Optional<OrdemManutencao> findByIdComQuarto(@Param("id") Long id);

    List<OrdemManutencao> findByEstadoOrderByAbertaEmDesc(EstadoManutencao estado);

    List<OrdemManutencao> findByQuartoIdOrderByAbertaEmDesc(Long quartoId);

    List<OrdemManutencao> findByEstadoIn(List<EstadoManutencao> estados);

    /** Listagem completa com quarto (anti-N+1), mais recentes primeiro. */
    @Query("""
            SELECT o FROM OrdemManutencao o
            JOIN FETCH o.quarto
            ORDER BY o.abertaEm DESC, o.id DESC
            """)
    Page<OrdemManutencao> buscarTodos(Pageable pageable);

    /** Listagem filtrada por estado, com quarto (anti-N+1). */
    @Query("""
            SELECT o FROM OrdemManutencao o
            JOIN FETCH o.quarto
            WHERE o.estado = :estado
            ORDER BY o.abertaEm DESC, o.id DESC
            """)
    Page<OrdemManutencao> buscarPorEstado(@Param("estado") EstadoManutencao estado, Pageable pageable);
}
