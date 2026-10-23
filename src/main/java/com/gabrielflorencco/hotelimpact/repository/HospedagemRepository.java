package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HospedagemRepository extends JpaRepository<Hospedagem, Long> {

    boolean existsByReservaId(Long reservaId);

    boolean existsByQuartoId(Long quartoId);

    boolean existsByQuartoIdAndEstado(Long quartoId, EstadoHospedagem estado);

    Optional<Hospedagem> findByReservaId(Long reservaId);

    /** Uma única hospedagem EM_ANDAMENTO por quarto (reforçada por partial unique index). */
    Optional<Hospedagem> findByQuartoIdAndEstado(Long quartoId, EstadoHospedagem estado);

    @Query("""
            SELECT h FROM Hospedagem h
            JOIN FETCH h.reserva r
            JOIN FETCH r.hospede
            JOIN FETCH h.quarto
            WHERE h.id = :id
            """)
    Optional<Hospedagem> findByIdComDetalhes(@Param("id") Long id);

    List<Hospedagem> findByEstadoOrderByCheckInRealDesc(EstadoHospedagem estado);
}
