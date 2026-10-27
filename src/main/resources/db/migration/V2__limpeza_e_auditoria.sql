-- V2 — Integridade do módulo de limpeza (e preparação da auditoria):
-- no máximo UMA tarefa de limpeza aberta (PENDENTE/EM_EXECUCAO) por quarto,
-- o mesmo padrão de defesa em profundidade do índice parcial de hospedagem
-- ativa (uk_hospedagem_quarto_em_andamento). A abertura acontece apenas no
-- check-out, que exige quarto OCUPADO; o índice é a última linha contra
-- duplicidade sob concorrência.

CREATE UNIQUE INDEX uk_tarefa_limpeza_aberta
    ON public.tarefa_limpeza (quarto_id)
    WHERE ((estado)::text IN ('PENDENTE', 'EM_EXECUCAO'));
