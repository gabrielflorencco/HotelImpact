package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.Pagamento;
import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByHospedagemIdOrderByDataHoraAsc(Long hospedagemId);

    List<Pagamento> findByEstadoAndDataHoraBetween(EstadoPagamento estado, LocalDateTime inicio, LocalDateTime fim);

    /** Pagamentos confirmados da hospedagem: os que contam como recebidos. */
    List<Pagamento> findByHospedagemIdAndEstado(Long hospedagemId, EstadoPagamento estado);

    /** Garante que o estorno só ocorre sobre pagamento da própria hospedagem. */
    Optional<Pagamento> findByIdAndHospedagemId(Long id, Long hospedagemId);
}
