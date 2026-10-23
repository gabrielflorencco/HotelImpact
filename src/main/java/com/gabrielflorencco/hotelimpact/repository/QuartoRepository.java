package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuartoRepository extends JpaRepository<Quarto, Long> {

    boolean existsByNumeroAndIdNot(String numero, Long id);

    boolean existsByNumero(String numero);

    long countByCategoriaId(Long categoriaId);

    Optional<Quarto> findByNumero(String numero);

    List<Quarto> findAllByAtivoTrueOrderByNumeroAsc();

    long countByEstadoAndAtivoTrue(EstadoQuarto estado);

    long countByAtivoTrue();

    /** Consulta com JOIN FETCH para evitar N+1 nas listagens (categoria é exibida sempre). */
    @Query("""
            SELECT q FROM Quarto q
            JOIN FETCH q.categoria
            ORDER BY q.numero ASC
            """)
    List<Quarto> findAllComCategoria();

    /**
     * Busca com filtros opcionais: número (parcial), estado operacional e categoria.
     * Parâmetros nulos são ignorados; os casts evitam vinculação bytea no PostgreSQL.
     */
    @Query("""
            SELECT q FROM Quarto q
            JOIN FETCH q.categoria
            WHERE (cast(:numero AS String) IS NULL
                   OR LOWER(q.numero) LIKE LOWER(CONCAT('%', cast(:numero AS String), '%')))
              AND (:estado IS NULL OR q.estado = :estado)
              AND (:categoriaId IS NULL OR q.categoria.id = :categoriaId)
            ORDER BY q.numero ASC
            """)
    List<Quarto> buscar(@Param("numero") String numero,
                        @Param("estado") EstadoQuarto estado,
                        @Param("categoriaId") Long categoriaId);

    @Query("""
            SELECT q FROM Quarto q
            JOIN FETCH q.categoria
            WHERE q.id = :id
            """)
    Optional<Quarto> findByIdComCategoria(@Param("id") Long id);
}
