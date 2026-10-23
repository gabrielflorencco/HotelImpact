# HotelImpact

Sistema de gestão hoteleira (PMS — Property Management System) desenvolvido como
trabalho acadêmico de desenvolvimento de software.

## Stack

- Java 21 · Spring Boot (4.2.0-M2) · Maven
- Spring MVC + Thymeleaf (renderização no servidor)
- Spring Data JPA + Hibernate
- Flyway (migrações de esquema)
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

A suíte cobre validação de formulários (unitário), regras de negócio contra
PostgreSQL real (integração) e a camada web com MockMvc (Post/Redirect/Get,
validação, renderização e páginas de erro).

## Integração contínua

O GitHub Actions (`.github/workflows/ci.yml`) roda a suíte completa a cada
push e pull request em `main`, com Java 21 e o Docker do runner para os
Testcontainers.

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
entidades, persistência, layout e infraestrutura de testes), Fase 3 (cadastros
de categorias, quartos e hóspedes) e Fase 4 (reservas e hospedagens) concluídas.

O ciclo operacional completo está disponível: criação de reservas com
verificação de disponibilidade, capacidade efetiva e total calculado no
servidor; confirmação, cancelamento e registro de no-show; check-in a partir
de reservas elegíveis; check-out que conclui a reserva e envia o quarto para
limpeza; conclusão de limpeza que devolve o quarto ao estado disponível;
lançamento/cancelamento de consumos na conta da hospedagem, com preço
histórico, baixa de estoque e estorno lógico que restitui o estoque; e
registro/estorno de pagamentos com saldo devedor calculado no servidor
(reserva + consumos ativos − pagamentos confirmados). O check-out exige a
conta quitada; com saldo pendente, é preciso registrar a justificativa do
encerramento excepcional, que marca a hospedagem como cancelada.

O catálogo de produtos e serviços tem gestão própria (nome único, preço,
controle de estoque apenas para produtos, alerta de estoque baixo e
exclusão bloqueada por histórico de consumos). As ordens de manutenção
cobrem o ciclo completo: abertura proibida com hospedagem em andamento ou
outra ordem ativa, prioridade CRÍTICA interdita o quarto, execução põe o
quarto em manutenção e a conclusão/cancelamento restauram o estado apenas
quando foi causado pela própria ordem.

O painel inicial (Fase 6) exibe indicadores reais calculados no banco:
ocupação atual, quartos por estado, chegadas e saídas previstas para o
dia, receita confirmada do dia e alerta de itens com estoque baixo.
Páginas de relatórios históricos serão avaliadas como evolução futura.

## Autenticação e execução em produção

Todo o sistema exige usuário autenticado (login por formulário, logout e
proteção CSRF — padrões do Spring Security). Em desenvolvimento, o usuário
`recepcao` com a senha `recepcao-dev` é criado automaticamente; em produção
defina as credenciais por variável de ambiente:

| Variável | Padrão local | Uso |
|---|---|---|
| `HOTELIMPACT_USER` | `recepcao` | usuário de acesso |
| `HOTELIMPACT_PASSWORD` | `recepcao-dev` | senha (troque em produção) |
| `SPRING_PROFILES_ACTIVE` | — | ative `prod` para validar o esquema contra o Flyway (`ddl-auto=validate`) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/hotelimpact` | conexão do banco |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | `hotelimpact` / `hotelimpact_dev` | credenciais do banco |
| `PORT` | `8080` | porta HTTP |
| `APP_SEED_DEMO` | `false` | dados fictícios (apenas desenvolvimento) |

## Migrações de esquema (Flyway)O esquema é versionado por migrações Flyway em `db/migration`. A `V1` é a
fotografia fiel do esquema existente (gerada por `pg_dump` do próprio
esquema criado pelo Hibernate, incluindo as constraints específicas do
PostgreSQL). Bancos novos executam a `V1` normalmente; bancos já populados
anteriores ao Flyway recebem baseline na versão 1
(`spring.flyway.baseline-on-migrate=true`) e tratam a `V1` como já
aplicada — nenhum dado é tocado. Evoluções futuras do esquema entram como
`V2__...`, `V3__...` e assim por diante; nunca edite uma migração já
publicada. O `ddl-auto` do Hibernate permanece como rede de segurança
até a migração ser validada em produção.
