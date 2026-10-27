# HotelImpact — Lacunas, limitações e riscos conhecidos

> Levantamento crítico por leitura do código, dos testes e do banco
> (out/2025). Classificação de gravidade: **alta** (integridade/segurança),
> **média** (funcionalidade/consistência), **baixa** (polimento/proposta).
> Distingue **defeito comprovado** de **risco potencial** e de
> **melhoria recomendada**. Nada aqui foi "arrumado" por conveniência — verifique
> antes de apresentar.

## 1. Lacunas de domínio (entidades sem ciclo completo)

| # | Item | Gravidade | Evidência | Impacto |
|---|---|---|---|---|
| G-01 | **`TarefaLimpeza` é uma entidade órfã**: existe no banco (tabela + CHECK + índices) e no JPA, mas não possui service, controller, DTO nem interface. A "limpeza" real é apenas o estado do quarto (`LIMPEZA → DISPONIVEL`) | média | `entity/TarefaLimpeza.java`; V1 cria a tabela; nenhum uso além da entidade | A promessa de histórico de limpeza (comentário na entidade) não se cumpre; na apresentação, apresentar limpeza como **estado do quarto**, não como módulo |
| G-02 | **Não existe edição de reserva**: datas, quarto, hóspede e valores não podem ser alterados após a criação; a única saída é cancelar e recriar | média | `ReservaController` não expõe edição | Operação comum de recepção exige dois passos; documentar como limitação consciente do escopo acadêmico |
| G-03 | **Categoria não possui desativação**: `BR-CAT-002` recomenda "desative-a" na mensagem de exclusão, mas não há `alternarAtivo` de categoria (quartos e itens têm) | média | `CategoriaQuartoService` tem só listar/buscar/salvar/excluir | Mensagem orienta uma ação inexistente — inconsistência comprovada entre mensagem e funcionalidade |
| G-04 | **Sem gestão de usuários/auditoria**: um único usuário (via env), papel único `RECEPCAO`; nenhum registro de *quem* executou check-in/check-out/pagamento | média | `SecurityConfig`; nenhuma entidade de usuário | Para apresentação acadêmica é aceitável; para uso real é pendência conhecida |

## 2. Configuração e infraestrutura

| # | Item | Gravidade | Evidência | Impacto |
|---|---|---|---|---|
| C-01 | **`spring.jpa.hibernate.ddl-auto=update` no perfil default** (rede de segurança de dev); o `prod` usa `validate` | média (risco de drift em dev) | `application.properties` vs `application-prod.properties` | O esquema efetivo em dev pode divergir da V1 se o Hibernate criar algo novo; a evolução planejada é desligar após validação em produção |
| C-02 | **Spring Boot 4.2.0-M2 é milestone** (pré-lançamento) | média | `pom.xml` | Migração para GA pendente; CI + Flyway reduzem o risco de mudança |
| C-03 | **Deploy real não executado**: perfil `prod` pronto, mas sem Dockerfile de app, provedor definido ou pipeline de release | baixa (para o escopo) | ausência de configuração de deploy | Decisão de infraestrutura em aberto (não é defeito) |
| C-04 | **`ReservaController` injeta `HospedeRepository`/`QuartoRepository` diretamente** (para popular selects), quebrando a pureza camada controller→service→repository | baixa | código do controller | Aceitável como consulta read-only; refactor futuro via método de service |
| C-05 | **Dependabot não ativado**; `devtools` no pom com uso real não confirmado em runtime de produção | baixa | pom.xml; ausência de config | Melhoria recomendada |

## 3. Comportamentos que merecem atenção na apresentação

| # | Item | Gravidade | Evidência | Impacto |
|---|---|---|---|---|
| B-01 | **Saídas de hoje só listam reservas ativas**: um check-out já concluído (reserva `CONCLUIDA`) sai da lista de "saídas de hoje" do painel | baixa (comportamento documentado, não defeito) | `DashboardService.saidasDeHoje` usa `RESERVAS_ATIVAS` | Explicar como critério do domínio: a lista mostra saídas **previstas ainda não realizadas** |
| B-02 | **Pagamento em hospedagem ENCERRADA é permitido** (quitação tardia) — pode surpreender; apenas `CANCELADA` bloqueia | baixa (decisão de domínio) | `PagamentoService.registrar` + teste `…PermiteQuitacaoTardia` | Apresentar como feature (flexibilidade de cobrança), não como falha |
| B-03 | **Cancelamento de reserva exige ausência de check-in**, mas **não existe bloqueio no banco** — apenas no service (a EXCLUDE só olha estados ativos) | baixa (risco de corrida é irrelevante na prática monousuária) | `ReservaService.cancelar` | Não é explorável pela UI; nota técnica |
| B-04 | **Sem exclusão/estorno de consumo já cancelado ou reabertura de hospedagem** — estados finais são finais | — (design) | services | Apresentar como proteção do histórico |
| B-05 | **Estados vazios de filtros** e mensagens existem, mas não há testes de interface específicos para todos eles | baixa | suíte atual | Cobertura via validação manual das rodadas visuais |

## 4. Testes: onde a cobertura é menor

- Não há `HospedesWebTest` dedicado (a tela é coberta por integração de
  cadastro e pela validação manual/Playwright; o controller de hóspedes
  não possui slice próprio) — **melhoria recomendada**, gravidade baixa.
- Os cenários da massa de demonstração são testados por `DataSeederTest`,
  mas os **textos exatos** do painel (nomes de hóspedes em listas) não são
  asserted — apenas na validação por navegador. As projeções de leitura,
  porém, têm cobertura direta (`ViewsEContratosTest`: mapeamento e máscara
  de privacidade do CPF), e as máquinas de estado das entidades e o
  fechamento da conta têm testes de domínio puro
  (`TransicoesDeEstadoTest`, `TransicoesDeEstadoIITest`,
  `EstadoQuartoRecepcaoTest`, `ContaTest`).
- Nenhum teste automatizado de acessibilidade (contraste/teclado) na suíte;
  as auditorias foram feitas com scripts de validação nos ciclos de UI.

## 5. Não confirmado / desconhecido

- Uso real do `spring-boot-devtools` em runtime (aparece no pom como
  ferramenta de desenvolvimento; não influencia produção).
- Comportamento do `pg_upgrade` do container com o volume
  `hotelimpact_pgdata` entre versões maiores do PostgreSQL (configuração
  documentada no docker-compose, não exercitada).
- Origem exata da foto do hero (Unsplash, licença livre) — os metadados não
  acompanham o arquivo; recomenda-se registrar a origem no repositório.

## 6. Resumo por gravidade

- **Alta:** nenhuma lacuna de integridade/servidor comprovada. As proteções
  críticas (EXCLUDE, índice parcial, CHECKs, CSRF, transações) estão no banco
  e nos services, com testes.
- **Média (4):** G-01 (entidade órfã), G-02 (sem edição de reserva), G-03
  (desativação de categoria ausente), C-01/C-02 (ddl-auto de dev e Boot M2).
- **Baixa (5+):** itens de polimento, mensagens e propostas de melhoria
  descritos acima.
