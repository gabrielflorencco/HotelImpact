package com.gabrielflorencco.hotelimpact.enums;

/**
 * Operações de negócio auditadas: as transições críticas da operação do
 * hotel que merecem registro permanente de quem as executou.
 */
public enum OperacaoAuditoria {
    RESERVA_CRIADA,
    RESERVA_EDITADA,
    RESERVA_CANCELADA,
    RESERVA_NO_SHOW,
    CHECK_IN,
    CHECK_OUT,
    CONSUMO_LANCADO,
    CONSUMO_CANCELADO,
    PAGAMENTO_REGISTRADO,
    PAGAMENTO_ESTORNADO,
    LIMPEZA_INICIADA,
    LIMPEZA_CONCLUIDA,
    MANUTENCAO_ABERTA,
    MANUTENCAO_INICIADA,
    MANUTENCAO_CONCLUIDA,
    MANUTENCAO_CANCELADA
}
