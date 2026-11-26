# Documentação — Mekano Supplies Purchase Request Service API

Índice da documentação do serviço de **estoque, requisição de compra e NF de entrada** do Mekano, **Fase 4**.

> **Prefixo dos endpoints:** `/api/v4`

## Navegação

| Documento | Conteúdo |
|---|---|
| [`00-escopo.md`](00-escopo.md) | Objetivo, escopo, limites e **mudança de escopo do estoque** |
| [`01-arquitetura.md`](01-arquitetura.md) | Clean Architecture, módulos, dependências, portas e banco |
| [`02-plano-de-tarefas.md`](02-plano-de-tarefas.md) | Sprints, branches, tarefas, dependências e critérios de aceite |
| [`03-modelo-de-dados.md`](03-modelo-de-dados.md) | Tabelas, migrations finais, seed, estoque e movimentos |
| [`04-contratos-api.md`](04-contratos-api.md) | Endpoints públicos e internos, exemplos e erros |
| [`05-testes-e-postman.md`](05-testes-e-postman.md) | Migração dos testes do monólito e dos JSONs do Postman |
| [`06-operacao-local.md`](06-operacao-local.md) | Docker Compose, DBeaver, variáveis e troubleshooting |
| [`07-decisoes-e-riscos.md`](07-decisoes-e-riscos.md) | Decisões, ADRs, roles futuras e riscos |
| [`adr/ADR-003-ownership-estoque.md`](adr/ADR-003-ownership-estoque.md) | Explicação completa: estoque saiu do Registry e veio para o Supplies |
| [`adr/ADR-005-idempotencia-estoque.md`](adr/ADR-005-idempotencia-estoque.md) | Idempotência, ledger e transação local da NF |

## Regras gerais

- **Fase 4** do projeto Mekano.
- **API prefix:** `/api/v4`.
- **Branch atual de fundação:** `start`.
- **Nunca** push direto em `main`; cada sprint usa uma branch e um PR.
- **Exceção única:** commit inicial vazio em `main` para viabilizar o primeiro PR.
- **Autenticação fora de escopo:** `@RolesAllowed` comentado com `TODO`.
- **Estoque é deste serviço:** saldo, reservado, mínimo e movimentos.
- **Registry é catálogo:** a peça é referenciada por `peca_uuid`.
- **Migrations finais:** uma migration completa por tabela; sem migrations de correção nesta fase.
- **Testes e Postman:** migração/organização final na branch `test/sprint-5-testes-e-postman`.
