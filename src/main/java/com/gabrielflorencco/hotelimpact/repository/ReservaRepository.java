package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

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
}
