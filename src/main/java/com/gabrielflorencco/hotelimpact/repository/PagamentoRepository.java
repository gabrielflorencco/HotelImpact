package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.Pagamento;
import com.gabrielflorencco.hotelimpact.enums.EstadoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    /** Receita confirmada no intervalo [inicio, fim); zero quando não há pagamentos. */
    @Query("""
            SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p
            WHERE p.estado = :estado
              AND p.dataHora >= :inicio
              AND p.dataHora < :fim
            """)
    BigDecimal somarPorEstadoEntre(@Param("estado") EstadoPagamento estado,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fim") LocalDateTime fim);
}
