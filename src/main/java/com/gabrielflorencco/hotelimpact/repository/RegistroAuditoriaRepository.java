package com.gabrielflorencco.hotelimpact.repository;

import com.gabrielflorencco.hotelimpact.entity.RegistroAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    Page<RegistroAuditoria> findByOrderByDataHoraDesc(Pageable pageable);
}
