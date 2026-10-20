package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.TarefaLimpeza;
import com.gabrielflorencco.hotelimpact.enums.EstadoTarefaLimpeza;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarefaLimpezaRepository extends JpaRepository<TarefaLimpeza, Long> {

    List<TarefaLimpeza> findByEstadoOrderByCriadaEmAsc(EstadoTarefaLimpeza estado);

    List<TarefaLimpeza> findByQuartoIdOrderByCriadaEmDesc(Long quartoId);
}
