package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.OrdemManutencao;
import com.gabrielflorencco.hotelimpact.enums.EstadoManutencao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdemManutencaoRepository extends JpaRepository<OrdemManutencao, Long> {

    boolean existsByQuartoId(Long quartoId);

    List<OrdemManutencao> findByEstadoOrderByAbertaEmDesc(EstadoManutencao estado);

    List<OrdemManutencao> findByQuartoIdOrderByAbertaEmDesc(Long quartoId);

    List<OrdemManutencao> findByEstadoIn(List<EstadoManutencao> estados);
}
