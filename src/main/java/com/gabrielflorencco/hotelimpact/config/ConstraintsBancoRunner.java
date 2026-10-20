package com.gabrielflorencco.hotelimpact.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Aplica restrições de integridade específicas do PostgreSQL que o ddl-auto do
 * Hibernate não expressa: constraint de exclusão para impedir reservas ativas
 * sobrepostas no mesmo quarto, partial unique index para impedir duas
 * hospedagens simultâneas no mesmo quarto e CHECK de estoque não negativo.
 *
 * Todos os comandos são idempotentes e executados antes do DataSeeder.
 * Em bancos gerenciados sem permissão para a extensão btree_gist, a aplicação
 * segue funcionando com a verificação de disponibilidade na camada de serviço.
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class ConstraintsBancoRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        executar("CREATE EXTENSION IF NOT EXISTS btree_gist", "extensão btree_gist");

        executar("""
                DO $$
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ex_reserva_quarto_periodo') THEN
                        ALTER TABLE reserva ADD CONSTRAINT ex_reserva_quarto_periodo
                        EXCLUDE USING gist (
                            quarto_id WITH =,
                            daterange(data_check_in, data_check_out, '[)') WITH &&
                        )
                        WHERE (estado IN ('PENDENTE', 'CONFIRMADA'));
                    END IF;
                END $$;
                """, "constraint de exclusão de reservas sobrepostas");

        executar("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_hospedagem_quarto_em_andamento
                ON hospedagem (quarto_id)
                WHERE estado = 'EM_ANDAMENTO';
                """, "partial unique index de hospedagem simultânea");

        executar("""
                DO $$
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_item_catalogo_estoque_nao_negativo') THEN
                        ALTER TABLE item_catalogo
                        ADD CONSTRAINT ck_item_catalogo_estoque_nao_negativo CHECK (estoque_atual >= 0);
                    END IF;
                END $$;
                """, "check de estoque não negativo");
    }

    private void executar(String sql, String descricao) {
        try {
            jdbcTemplate.execute(sql);
            log.info("Constraint do banco verificada/aplicada: {}", descricao);
        } catch (DataAccessException ex) {
            log.warn("Não foi possível aplicar '{}': {} (a aplicação continuará; "
                    + "a integridade ficará a cargo da camada de serviço)", descricao, ex.getMostSpecificCause().getMessage());
        }
    }
}
