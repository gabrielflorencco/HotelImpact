package com.gabrielflorencco.hotelimpact.entity;

import com.gabrielflorencco.hotelimpact.enums.OperacaoAuditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Evento de auditoria de operação de negócio: registro histórico imutável —
 * sem setters, criado no momento da operação e jamais alterado. A mesma
 * transação da operação audita garante consistência (a operação e o
 * registro persistem juntos ou nada persiste).
 *
 * Nenhum dado sensível é gravado: apenas o identificador do usuário
 * autenticado, a operação e a referência ao registro afetado.
 */
@Entity
@Table(name = "registro_auditoria", indexes = {
        @Index(name = "idx_auditoria_data", columnList = "data_hora"),
        @Index(name = "idx_auditoria_entidade", columnList = "entidade_tipo, entidade_id")
})
@Getter
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.validation.constraints.NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OperacaoAuditoria operacao;

    @jakarta.validation.constraints.NotNull
    @Column(nullable = false, length = 60)
    private String usuario;

    /** Tipo lógico da entidade afetada (ex.: "Reserva", "Hospedagem"). */
    @jakarta.validation.constraints.NotNull
    @Column(name = "entidade_tipo", nullable = false, length = 30)
    private String entidadeTipo;


    @Column(name = "entidade_id", nullable = false)
    private Long entidadeId;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    /** Contexto curto e não sensível (ex.: código da reserva, número do quarto). */
    @Column(length = 300)
    private String detalhe;

    /** Construtor de domínio: o evento nasce completo e imutável. */
    public RegistroAuditoria(OperacaoAuditoria operacao, String usuario,
                             String entidadeTipo, Long entidadeId,
                             LocalDateTime dataHora, String detalhe) {
        this.operacao = operacao;
        this.usuario = usuario;
        this.entidadeTipo = entidadeTipo;
        this.entidadeId = entidadeId;
        this.dataHora = dataHora;
        this.detalhe = detalhe == null || detalhe.isBlank() ? null : detalhe.trim();
    }

    /** Construtor exigido pelo JPA. */
    protected RegistroAuditoria() {
    }
}
