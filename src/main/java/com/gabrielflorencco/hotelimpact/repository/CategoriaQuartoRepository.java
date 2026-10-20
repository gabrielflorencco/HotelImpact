package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaQuartoRepository extends JpaRepository<CategoriaQuarto, Long> {

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    boolean existsByNomeIgnoreCase(String nome);

    Optional<CategoriaQuarto> findByNomeIgnoreCase(String nome);

    List<CategoriaQuarto> findAllByOrderByNomeAsc();
}
