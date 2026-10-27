package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.TarefaLimpeza;
import com.gabrielflorencco.hotelimpact.enums.EstadoTarefaLimpeza;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarefaLimpezaRepository extends JpaRepository<TarefaLimpeza, Long> {

    boolean existsByQuartoId(Long quartoId);

    List<TarefaLimpeza> findByEstadoOrderByCriadaEmAsc(EstadoTarefaLimpeza estado);

    List<TarefaLimpeza> findByQuartoIdOrderByCriadaEmDesc(Long quartoId);

    long countByEstado(EstadoTarefaLimpeza estado);

    java.util.Optional<TarefaLimpeza> findFirstByQuartoIdAndEstadoNotOrderByCriadaEmDesc(
            Long quartoId, EstadoTarefaLimpeza estado);

    List<TarefaLimpeza> findByEstadoNotOrderByCriadaEmDesc(EstadoTarefaLimpeza estado);

    long countByConcluidaEmBetween(java.time.LocalDateTime inicio, java.time.LocalDateTime fim);
}
