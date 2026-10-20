package com.gabrielflorencco.hotelimpact.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base para testes de integração: sobe um PostgreSQL real e efêmero via
 * Testcontainers (container único compartilhado entre as classes de teste).
 */
public abstract class AbstractIntegrationTest {

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
}
