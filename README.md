# HotelImpact

Sistema de gestão hoteleira (PMS — Property Management System) desenvolvido como
trabalho acadêmico de desenvolvimento de software.

## Stack

- Java 21 · Spring Boot (4.2.0-M2) · Maven
- Spring MVC + Thymeleaf (renderização no servidor)
- Spring Data JPA + Hibernate
- PostgreSQL (via Docker em desenvolvimento)
- Bootstrap 5 (vendorizado em `src/main/resources/static/vendor`)

## Execução local

Pré-requisitos: Java 21 e Docker.

```bash
# 1. Subir o banco de dados (porta 5433 no host)
docker compose up -d

# 2. Rodar a aplicação (opcionalmente com dados de demonstração)
APP_SEED_DEMO=true ./mvnw spring-boot:run

# 3. Acessar
# http://localhost:8080
```

Credenciais e endereço do banco são lidos de variáveis de ambiente, com padrões
adequados ao `docker-compose.yml` local:

| Variável | Padrão local |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/hotelimpact` |
| `SPRING_DATASOURCE_USERNAME` | `hotelimpact` |
| `SPRING_DATASOURCE_PASSWORD` | `hotelimpact_dev` |
| `PORT` | `8080` |
| `APP_SEED_DEMO` | `false` |

Os dados de demonstração são fictícios e a carga é idempotente (não duplica
registros ao reiniciar).

## Testes

Os testes de integração usam Testcontainers e exigem o daemon Docker ativo:

```bash
./mvnw test
```

## Estrutura

```
src/main/java/com/gabrielflorencco/hotelimpact/
├── config/       Configurações (constraints de banco, dados demo)
├── controller/   Controllers MVC
├── dto/          Objetos de formulário e de saída
├── entity/       Entidades JPA
├── enums/        Estados e tipos controlados
├── exception/    Exceções de negócio e tratamento centralizado
├── repository/   Interfaces Spring Data JPA
├── service/      Regras de negócio
└── validation/   Validadores (CPF)
```

## Situação do desenvolvimento

O projeto é desenvolvido em fases. Fase 1 (planejamento), Fase 2 (fundação:
entidades, persistência, layout e infraestrutura de testes) e Fase 3 (cadastros
de categorias, quartos e hóspedes) concluídas.

Fase 4 (reservas e hospedagens) em andamento: já é possível criar, listar,
detalhar, confirmar e cancelar reservas, com verificação de disponibilidade,
capacidade efetiva, exceção para datas passadas e total calculado no servidor.
Check-in/check-out (hospedagens), consumos, pagamentos, manutenção e relatórios
serão entregues nas próximas fases.
