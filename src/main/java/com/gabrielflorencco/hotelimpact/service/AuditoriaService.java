package com.gabrielflorencco.hotelimpact.service;

import com.gabrielflorencco.hotelimpact.entity.RegistroAuditoria;
import com.gabrielflorencco.hotelimpact.enums.OperacaoAuditoria;
import com.gabrielflorencco.hotelimpact.repository.RegistroAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Auditoria de operações de negócio: registra quem executou cada transição
 * crítica, na mesma transação da operação. O usuário vem do contexto de
 * segurança (Spring Security) com fallback "sistema" para execuções sem
 * sessão (seed, testes de serviço) — as entidades de negócio permanecem
 * desacopladas de HTTP e de segurança.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final RegistroAuditoriaRepository registros;

    /** Nome do usuário autenticado ou "sistema" quando não há sessão. */
    private String usuarioAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null && autenticacao.isAuthenticated()
                && !"anonymousUser".equals(autenticacao.getName())
                ? autenticacao.getName()
                : "sistema";
    }

    /** Registra o evento — obrigatório na mesma transação da operação auditada. */
    @Transactional
    public void registrar(OperacaoAuditoria operacao, String entidadeTipo,
                          Long entidadeId, String detalhe) {
        registros.save(new RegistroAuditoria(
                operacao, usuarioAtual(), entidadeTipo, entidadeId, LocalDateTime.now(), detalhe));
    }

    /** Atividades recentes para a recepção (histórico paginado). */
    @Transactional(readOnly = true)
    public Page<RegistroAuditoria> listarRecentes(Pageable pageable) {
        return registros.findByOrderByDataHoraDesc(pageable);
    }
}
