package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.Pagamento;
import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByHospedagemIdOrderByDataHoraAsc(Long hospedagemId);

    List<Pagamento> findByEstadoAndDataHoraBetween(EstadoPagamento estado, LocalDateTime inicio, LocalDateTime fim);
}
