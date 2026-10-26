# Documentação do HotelImpact

Índice dos documentos técnicos do projeto. Todos baseiam-se na leitura
sistemática do código, dos testes e do banco — quando algo não pôde ser
confirmado, está marcado no próprio texto.

## Ordem recomendada de leitura

| # | Documento | Para quê |
|---|---|---|
| 1 | [ARCHITECTURE.md](ARCHITECTURE.md) | **Comece aqui.** Tecnologias (com versões e onde são usadas), estrutura do projeto, padrões de projeto reais, modelo de dados com diagrama ER e constraints, segurança, interface e fluxos entre módulos. |
| 2 | [BUSINESS_RULES.md](BUSINESS_RULES.md) | As **regras de negócio** identificáveis no sistema (BR-XXX), com máquinas de estado, fórmulas, cenários passo a passo e matriz de rastreabilidade código↔teste. Use para estudar o comportamento e preparar provas/apresentações. |
| 3 | [IMPLEMENTATION_GAPS.md](IMPLEMENTATION_GAPS.md) | O que **não** existe ou está parcial: lacunas, riscos, inconsistências e melhorias recomendadas, classificadas por gravidade. Leia antes de apresentar para não ser surpreendido. |
| 4 | [PRESENTATION_GUIDE.md](PRESENTATION_GUIDE.md) | Roteiro de demonstração, discurso por público (técnico/não técnico), perguntas prováveis com respostas fundamentadas no código. |
| — | [../README.md](../README.md) | README do repositório: execução local, credenciais de desenvolvimento e visão geral rápida. |
| — | [design-references/](design-references/) | Imagens de referência visual usadas no redesign da interface (Dribbble). |

## Mapa rápido: "onde eu encontro…"

- **Uma regra específica** (ex.: o que o check-out exige) → `BUSINESS_RULES.md`, seção do domínio + matriz final.
- **Por que uma tecnologia foi adotada** → `ARCHITECTURE.md §2 e §7`; frases prontas em `PRESENTATION_GUIDE.md §7`.
- **Constraints do banco** (EXCLUDE, índice parcial, CHECKs) → `ARCHITECTURE.md §5.1` e `V1__esquema_base.sql`.
- **Como uma requisição percorre o sistema** → `ARCHITECTURE.md §3.1` (diagrama de sequência do check-in) e `§8` (dependências entre services).
- **Como a SPA funciona sem recarregar** → `ARCHITECTURE.md §7` e `controller/Pagina.java` / `config/HxUrlInterceptor.java` / `static/js/app.js`.
- **Limitações conhecidas** → `IMPLEMENTATION_GAPS.md`.
- **Como rodar localmente / seeds de demonstração** → `../README.md` e `ARCHITECTURE.md §10`.

## Convenções citadas na documentação

- **IDs de regras** (`BR-RES-002` etc.): estáveis; cite-os em provas e commits.
- **Caminhos**: relativos à raiz do repositório (ex.: `service/ReservaService.java`).
- **Status**: *implementada* / *parcialmente implementada* / *implícita* / *não implementada* — definidos em `BUSINESS_RULES.md`.
- Diagramas em **Mermaid** (renderizam no GitHub); evidências citam classes,
  constraints e nomes reais de testes.
