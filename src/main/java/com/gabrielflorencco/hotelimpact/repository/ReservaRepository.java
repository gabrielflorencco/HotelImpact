package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByQuartoId(Long quartoId);

    boolean existsByHospedeId(Long hospedeId);

    /**
     * Reservas ativas do quarto que conflitam com o período semiaberto
     * [inicio, fim). O banco reforça esta regra com constraint de exclusão.
     */
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.quarto.id = :quartoId
              AND r.estado IN :estadosAtivos
              AND r.dataCheckIn < :fim
              AND r.dataCheckOut > :inicio
            """)
    List<Reserva> findConflitantes(@Param("quartoId") Long quartoId,
                                   @Param("inicio") LocalDate inicio,
                                   @Param("fim") LocalDate fim,
                                   @Param("estadosAtivos") List<EstadoReserva> estadosAtivos);

    /**
     * Conflitos para a edição: ignora a própria reserva — o período atual
     * dela não conflita consigo mesma (semiaberto).
     */
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.quarto.id = :quartoId
              AND r.id <> :reservaId
              AND r.estado IN :estadosAtivos
              AND r.dataCheckIn < :fim
              AND r.dataCheckOut > :inicio
            """)
    List<Reserva> findConflitantesExceto(@Param("quartoId") Long quartoId,
                                         @Param("reservaId") Long reservaId,
                                         @Param("inicio") LocalDate inicio,
                                         @Param("fim") LocalDate fim,
                                         @Param("estadosAtivos") List<EstadoReserva> estadosAtivos);

    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.hospede
            JOIN FETCH r.quarto
            WHERE r.id = :id
            """)
    Optional<Reserva> findByIdComDetalhes(@Param("id") Long id);

    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.hospede
            JOIN FETCH r.quarto
            WHERE r.codigo = :codigo
            """)
    Optional<Reserva> findByCodigoComDetalhes(@Param("codigo") String codigo);

    // Sufixo Equals necessário: o "In" de dataCheckIn seria parseado como operador de coleção.
    List<Reserva> findByEstadoInAndDataCheckInEquals(List<EstadoReserva> estados, LocalDate dataCheckIn);

    /** Listagem completa com hóspede e quarto (anti-N+1), mais recentes primeiro. */
    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.hospede
            JOIN FETCH r.quarto
            ORDER BY r.dataCheckIn DESC, r.id DESC
            """)
    Page<Reserva> buscarTodos(Pageable pageable);

    /** Listagem filtrada por estado, com hóspede e quarto (anti-N+1). */
    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.hospede
            JOIN FETCH r.quarto
            WHERE r.estado = :estado
            ORDER BY r.dataCheckIn DESC, r.id DESC
            """)
    Page<Reserva> buscarPorEstado(@Param("estado") EstadoReserva estado, Pageable pageable);

    /** Contagem por prefixo, usada na geração do código sequencial (RES-ANO-NNNNNN). */
    long countByCodigoStartingWith(String prefixo);

    /** Chegadas previstas para a data: reservas ativas com check-in naquele dia. */
    // Sufixo Equals necessário: o "In" de DataCheckIn seria parseado como operador de coleção.
    long countByEstadoInAndDataCheckInEquals(List<EstadoReserva> estados, LocalDate dataCheckIn);

    /** Saídas previstas para a data: reservas ativas com check-out naquele dia. */
    // Sufixo Equals pelo mesmo motivo do método acima.
    long countByEstadoInAndDataCheckOutEquals(List<EstadoReserva> estados, LocalDate dataCheckOut);

    /** Reservas criadas no intervalo semiaberto [inicio, fim). */
    long countByDataCriacaoBetween(java.time.LocalDateTime inicio, java.time.LocalDateTime fim);

    /** Reservas por estado criadas no intervalo semiaberto [inicio, fim). */
    long countByEstadoAndDataCriacaoBetween(EstadoReserva estado,
                                           java.time.LocalDateTime inicio, java.time.LocalDateTime fim);

    /** Chegadas do dia: reservas ativas com check-in na data, com hóspede e quarto (anti-N+1). */
    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.hospede
            JOIN FETCH r.quarto
            WHERE r.estado IN :estados
              AND r.dataCheckIn = :dia
            ORDER BY r.id DESC
            """)
    List<Reserva> buscarAtivasPorDataCheckInComDetalhes(@Param("estados") List<EstadoReserva> estados,
                                                        @Param("dia") LocalDate dia);

    /** Saídas do dia: reservas ativas com check-out na data, com hóspede e quarto (anti-N+1). */
    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.hospede
            JOIN FETCH r.quarto
            WHERE r.estado IN :estados
              AND r.dataCheckOut = :dia
            ORDER BY r.id DESC
            """)
    List<Reserva> buscarAtivasPorDataCheckOutComDetalhes(@Param("estados") List<EstadoReserva> estados,
                                                        @Param("dia") LocalDate dia);
}
