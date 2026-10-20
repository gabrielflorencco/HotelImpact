package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.Hospede;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HospedeRepository extends JpaRepository<Hospede, Long> {

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByCpf(String cpf);

    /**
     * Busca por nome, CPF, telefone ou e-mail (qualquer campo informado, parcial e sem
     * distinguir maiúsculas). Campos vazios são ignorados.
     */
    @Query("""
            SELECT h FROM Hospede h
            WHERE (:nome IS NULL OR LOWER(h.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
              AND (:cpf IS NULL OR h.cpf LIKE CONCAT('%', :cpf, '%'))
              AND (:telefone IS NULL OR h.telefone LIKE CONCAT('%', :telefone, '%'))
              AND (:email IS NULL OR LOWER(h.email) LIKE LOWER(CONCAT('%', :email, '%')))
            ORDER BY h.nome ASC
            """)
    Page<Hospede> buscar(@Param("nome") String nome,
                         @Param("cpf") String cpf,
                         @Param("telefone") String telefone,
                         @Param("email") String email,
                         Pageable pageable);
}
