package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.repository.CategoriaQuartoRepository;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validação da migração Flyway (Fase 7) em três fases, num container próprio
 * para controlar o histórico: (1) banco novo — V1 cria o esquema completo e
 * o JPA opera sobre ele sem ddl-auto; (2) banco "antigo" — esquema criado
 * pelo Hibernate sem Flyway, com dados e sem histórico de migração;
 * (3) migração do banco populado — baseline na versão 1 salta V1, preserva
 * os dados e registra a linha de histórico.
 */
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class MigracaoFlywayTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"))
                    .withDatabaseName("hotelimpact")
                    .withUsername("teste")
                    .withPassword("teste");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registrarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Nested
    @Order(1)
    @SpringBootTest(properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.baseline-on-migrate=true",
            "spring.flyway.baseline-version=1",
            "spring.jpa.hibernate.ddl-auto=none",
            "app.seed-demo=false",
            "app.test.fase-migracao=banco-novo"})
    class BancoNovo {

        @Autowired
        private JdbcTemplate jdbc;

        @Autowired
        private CategoriaQuartoRepository categorias;

        @Test
        void v1CriaOEsquemaCompletoFuncionalParaOJpa() {
            Integer aplicadas = jdbc.queryForObject(
                    "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
                    Integer.class);
            assertThat(aplicadas).isGreaterThanOrEqualTo(1);

            // O esquema vindo só do V1 sustenta operações JPA sem ddl-auto.
            assertThat(categorias.existsByNomeIgnoreCase("Categoria Flyway Banco Novo")).isFalse();
            categorias.save(categoria("Categoria Flyway Banco Novo"));
            assertThat(categorias.existsByNomeIgnoreCase("Categoria Flyway Banco Novo")).isTrue();

            // Constraints específicas do PostgreSQL vieram no V1.
            Integer exclusao = jdbc.queryForObject(
                    "SELECT count(*) FROM pg_constraint WHERE conname = 'ex_reserva_quarto_periodo'",
                    Integer.class);
            assertThat(exclusao).isEqualTo(1);
            Integer indiceParcial = jdbc.queryForObject(
                    "SELECT count(*) FROM pg_indexes WHERE indexname = 'uk_hospedagem_quarto_em_andamento'",
                    Integer.class);
            assertThat(indiceParcial).isEqualTo(1);
        }
    }

    @Nested
    @Order(2)
    @SpringBootTest(properties = {
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=update",
            "app.seed-demo=false",
            "app.test.fase-migracao=banco-antigo"})
    class BancoAntigo {

        @Autowired
        private JdbcTemplate jdbc;

        @Autowired
        private CategoriaQuartoRepository categorias;

        @Test
        void simulaBancoPopuladoAnteriorAoFlyway() {
            // Sem histórico: como qualquer banco criado antes da Fase 7.
            jdbc.execute("DROP TABLE IF EXISTS flyway_schema_history");

            categorias.save(categoria("Categoria Antiga Pré-Flyway"));

            Boolean semHistorico = jdbc.queryForObject(
                    "SELECT to_regclass('flyway_schema_history') IS NULL", Boolean.class);
            assertThat(semHistorico).isTrue();
        }
    }

    @Nested
    @Order(3)
    @SpringBootTest(properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.baseline-on-migrate=true",
            "spring.flyway.baseline-version=1",
            "spring.jpa.hibernate.ddl-auto=none",
            "app.seed-demo=false",
            "app.test.fase-migracao=banco-populado"})
    class BancoPopuladoMigrado {

        @Autowired
        private JdbcTemplate jdbc;

        @Autowired
        private CategoriaQuartoRepository categorias;

        @Test
        void baselineSaltaV1EPreservaOsDados() {
            assertThat(categorias.existsByNomeIgnoreCase("Categoria Antiga Pré-Flyway"))
                    .as("dados anteriores à migração continuam legíveis")
                    .isTrue();

            Integer baseline = jdbc.queryForObject(
                    "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
                    Integer.class);
            assertThat(baseline).isGreaterThanOrEqualTo(1);
        }
    }

    private CategoriaQuarto categoria(String nome) {
        CategoriaQuarto c = new CategoriaQuarto();
        c.setNome(nome);
        c.setCapacidadeMaxima(2);
        c.setQuantidadeCamas(1);
        c.setPrecoBase(new BigDecimal("200.00"));
        c.setAtivo(true);
        return c;
    }
}
