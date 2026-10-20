package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.Consumo;
import com.gabrielflorencco.hotelimpact.enums.EstadoConsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConsumoRepository extends JpaRepository<Consumo, Long> {

    @Query("""
            SELECT c FROM Consumo c
            JOIN FETCH c.item
            WHERE c.hospedagem.id = :hospedagemId
            ORDER BY c.dataHora DESC
            """)
    List<Consumo> findByHospedagemIdComItem(@Param("hospedagemId") Long hospedagemId);

    Optional<Consumo> findByIdAndHospedagemId(Long id, Long hospedagemId);

    List<Consumo> findByHospedagemIdAndEstado(Long hospedagemId, EstadoConsumo estado);
}
