package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.Hospede;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HospedeRepository extends JpaRepository<Hospede, Long> {

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByCpf(String cpf);

    Optional<Hospede> findByNome(String nome);

    /**
     * Busca por um único termo em nome, telefone ou e-mail (parcial) e, em
     * paralelo, por CPF com o termo reduzido a dígitos (termoCpf). Os casts
     * evitam que o Hibernate vincule parâmetros nulos como bytea no PostgreSQL.
     */
    @Query("""
            SELECT h FROM Hospede h
            WHERE (cast(:termo AS String) IS NULL
                   OR LOWER(h.nome) LIKE LOWER(CONCAT('%', cast(:termo AS String), '%'))
                   OR h.telefone LIKE CONCAT('%', cast(:termo AS String), '%')
                   OR LOWER(h.email) LIKE LOWER(CONCAT('%', cast(:termo AS String), '%'))
                   OR (cast(:termoCpf AS String) IS NOT NULL
                       AND h.cpf LIKE CONCAT('%', cast(:termoCpf AS String), '%')))
            ORDER BY h.nome ASC
            """)
    Page<Hospede> buscar(@Param("termo") String termo,
                         @Param("termoCpf") String termoCpf,
                         Pageable pageable);
}
