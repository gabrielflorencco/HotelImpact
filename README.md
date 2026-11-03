# HotelImpact

Sistema de gestão hoteleira (PMS — Property Management System) desenvolvido como trabalho acadêmico de desenvolvimento de software.

---

## 📦 Stack Tecnológica

| Categoria | Tecnologias |
|-----------|-------------|
| **Linguagem** | Java 21 |
| **Framework** | Spring Boot 4.2.0-M2 |
| **Build** | Maven |
| **Web** | Spring MVC, Thymeleaf (renderização no servidor) |
| **Persistência** | Spring Data JPA, Hibernate, PostgreSQL |
| **Migrações** | Flyway |
| **CSS/Design** | Bootstrap 5 (vendorizado), CSS custom properties em `static/css/site.css` |
| **Testes** | JUnit 5, Mockito, Testcontainers |

---

## 🏗️ Arquitetura e Organização

O projeto segue uma arquitetura layered com pacotes por camada:

```
src/main/java/com/gabrielflorencco/hotelimpact/
├── config/           # Constraints de banco, configuração de dados demo
├── controller/       # Controllers MVC (Spring)
├── dto/              # Objetos de formulário e de saída
├── entity/           # Entidades JPA
├── enums/            # Estados e tipos controlados (EstadoQuarto, etc.)
├── exception/        # Exceções de negócio e tratamento centralizado
├── repository/       # Interfaces Spring Data JPA
├── service/          # Regras de negócio
└── validation/       # Validadores (CPF)
```

### Migrações Flyway

- `db/migration/`: Versionado pelas migrações V1, V2, V3...
- V1: Fotografia fiel do esquema existente (pg_dump + Hibernate constraints PostgreSQL)
- Baseline: Bancos pré-Flyway recebem baseline na versão 1
- `ddl-auto=validate` em produção (Hibernate valida contra migrações)

---

## 🚀 Execução Local

Pré-requisitos: Java 21 e Docker.

```bash
# 1. Subir o banco de dados (porta 5433 no host)
docker compose up -d

# 2. Rodar a aplicação (opcionalmente com dados de demonstração)
APP_SEED_DEMO=true ./mvnw spring-boot:run

# 3. Acessar
# http://localhost:8080          → site institucional público
# http://localhost:8080/login    → autenticação da recepção
# http://localhost:8080/painel   → área administrativa (após login)
```

### Credenciais

| Variável | Padrão local | Uso |
|----------|--------------|-----|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/hotelimpact` | URL do banco |
| `SPRING_DATASOURCE_USERNAME` | `hotelimpact` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | `hotelimpact_dev` | Senha do banco |
| `HOTELIMPACT_USER` | `recepcao` | Usuário de acesso (dev) |
| `HOTELIMPACT_PASSWORD` | `recepcao-dev` | Senha de acesso (dev) |
| `PORT` | `8080` | Porta HTTP |
| `APP_SEED_DEMO` | `false` | Dados fictícios (apenas desenvolvimento) |

> ⚠️ **Importante**: Em produção, troque as credenciais padrão pelas definidas por variáveis de ambiente. O usuário `recepcao` com senha `recepcao-dev` é criado automaticamente em desenvolvimento.

---

## 🧪 Testes

Os testes de integração usam **Testcontainers** e exigem o daemon Docker ativo:

```bash
./mvnw --batch-mode test
```

### Cobertura de testes

A suíte cobre **244 testes** em quatro níveis:

| Nível | Descrição |
|-------|-----------|
| **Domínio puro** | Máquinas de estado, conta, limpeza, edição de reserva |
| **Validação de formulários** | Regras de negócio contra PostgreSQL real (integração) |
| **Slices web** | Camada web com MockMvc (Post/Redirect/Get, validação, páginas de erro) |
| **Integração** | PostgreSQL real e efêmero (Testcontainers) |

### Funcionalidades recentes (via migrations V2/V3)

- Módulo de limpeza com histórico e índice único parcial (V2)
- Edição segura de reservas (BR-RES-009)
- Desativação de categorias (BR-CAT-003)
- Auditoria de operações críticas (BR-AUD-001)

---

## 📐 Documentação e Conceitos de POO

A documentação técnica completa está em [`docs/`](docs/) — comece pelo [índice](docs/README.md):

- Arquitetura do sistema
- Regras de negócio rastreáveis (BR-XXX)
- Lacunas conhecidas
- **Guia de apresentação**: mapeia os conceitos de orientação a objetos demonstráveis no código real

### Conceitos de POO mapeados no código

| Conceito | Demonstração no Código |
|----------|------------------------|
| Composição `@ManyToOne` | Entre as entidades |
| Máquinas de estado encapsuladas | `Reserva.confirmar()/reprogramar()`, `Quarto.ocupar()`, `Hospedagem.encerrar(...)` |
| Interface de domínio `Lancamento` | Implementações `Consumo` e `Pagamento` |
| Value object `Conta` | Cálculo genérico `Fechamento` |
| Enum polimórfico `EstadoQuarto` | Sobrecarga de negócio `Hospedagem.encerrar(BigDecimal)` / `encerrar(BigDecimal, String)` |
| Auditoria em registro imutável | Operações críticas (sem herança artificial nem downcasting) |

---

## 🗄️ Situação do Desenvolvimento

O projeto é desenvolvido em fases:

| Fase | Status | Descrição |
|------|--------|-----------|
| **Fase 1** | ✅ Planejamento | — |
| **Fase 2** | ✅ Fundação | Entidades, persistência, layout e infraestrutura de testes |
| **Fase 3** | ✅ Cadastros | Categorias, quartos e hóspedes |
| **Fase 4** | ✅ Reservas e hospedagens | Ciclo completo: check-in, check-out, confirmação, cancelamento |

### Ciclo Operacional Completo

1. **Criação de reservas** — Verificação de disponibilidade, cálculo de capacidade efetiva e total no servidor
2. **Confirmação e cancelamento** — Com validação de período e desconto; recálculo do total
3. **Check-in** — A partir de reservas elegíveis
4. **Check-out** — Conclui a reserva e envia o quarto para limpeza; exige conta quitada
5. **Conclusão de limpeza** — Devolve o quarto ao estado disponível
6. **Lançamento/cancelamento de consumos** — Com preço histórico, baixa de estoque e estorno lógico
7. **Registro/estorno de pagamentos** — Saldo devedor calculado no servidor (reserva + consumos ativos − pagamentos confirmados)

### Módulos Adicionais

- **Catálogo de produtos e serviços** — Gestão própria (nome único, preço, controle de estoque, alerta de estoque baixo, exclusão bloqueada por histórico)
- **Ordens de manutenção** — Ciclo completo: abertura proibida com hospedagem em andamento ou ordem ativa; prioridade CRÍCTICA interdita o quarto; execução põe quarto em manutenção; conclusão/cancelamento apenas quando causado pela própria ordem
- **Painel inicial (Fase 6)** — Indicadores reais: ocupação atual, quartos por estado, chegadas/saídas previstas, receita confirmada do dia, alerta de itens com estoque baixo
- **Módulo de relatórios** — Resumo de qualquer período: receita confirmada, check-ins/check-outs reais, reservas criadas (com cancelamentos e no-shows), lista de hospedagens iniciadas no período

---

## 🔐 Autenticação e Execução em Produção

Todo o sistema exige usuário autenticado (login por formulário próprio em pt-BR, logout na barra superior e proteção CSRF — padrões do Spring Security).

### Variáveis de ambiente para produção

| Variável | Padrão local | Uso |
|----------|--------------|-----|
| `HOTELIMPACT_USER` | `recepcao` | Usuário de acesso |
| `HOTELIMPACT_PASSWORD` | `recepcao-dev` | Senha (troque em produção) |
| `SPRING_PROFILES_ACTIVE` | — | Ative `prod` para validar o esquema contra o Flyway (`ddl-auto=validate`) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/hotelimpact` | Conexão do banco |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | `hotelimpact` / `hotelimpact_dev` | Credenciais do banco |
| `PORT` | `8080` | Porta HTTP |
| `APP_SEED_DEMO` | `false` | Dados fictícios (apenas desenvolvimento) |

---

## 📦 Integração Contínua

O GitHub Actions (`.github/workflows/ci.yml`) roda a suíte completa de testes a cada push e pull request em `main`, com Java 21 e o Docker do runner para os Testcontainers.

---

## 📁 Estrutura Completa

```
src/main/java/com/gabrielflorencco/hotelimpact/
├── config/                   # Configurações (constraints de banco, dados demo)
├── controller/               # Controllers MVC
├── dto/                      # Objetos de formulário e de saída
├── entity/                   # Entidades JPA
├── enums/                    # Estados e tipos controlados
├── exception/                # Exceções de negócio e tratamento centralizado
├── repository/               # Interfaces Spring Data JPA
├── service/                  # Regras de negócio
└── validation/               # Validadores (CPF)
```

```
docs/
├── README.md                 # Índice da documentação técnica
├── arquitetura.md            # Arquitetura do sistema
├── regras-negocio.md         # Regras de negócio rastreáveis (BR-XXX)
├── lacunas-conhecidas.md     # Lacunas e aberturas
└── poo-guide.md              # Guia de apresentação de conceitos POO
```

```
db/migration/
├── V1__...sql                # Migração baseline (gerada por pg_dump)
├── V2__...sql                # Evoluções futuras do esquema
├── V3__...sql
└── ...
```

---

## 📜 Licença

Este projeto está licenciado sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

## 👨‍💻 Autor

**Gabriel Florenço** — Desenvolvedor Java / Spring Boot  
GitHub: [@gabrielflorencco](https://github.com/gabrielflorencco)  
Contexto: Projeto acadêmico avançado demonstrando arquitetura Clean Architecture, DDD, Testcontainers e CI/CD integrados.

---

**Última atualização:** 2026-11-02  
**Status:** Em desenvolvimento — Fases 1-4 concluídas. Próximas fases focadas em relatórios e módulo de manutenção.