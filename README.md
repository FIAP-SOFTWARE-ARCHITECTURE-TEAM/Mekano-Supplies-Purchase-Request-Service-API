# Mekano Supplies Purchase Request Service API

API de **estoque, requisição de compra e NF de entrada** do projeto **Mekano** — **Sprint 2**.

> **Status atual:** Sprint 2 (estoque e ciclo da requisição). Requisições, transições,
> movimentos idempotentes e consulta de estoque estão implementados. NF de entrada
> pertence à Sprint 3.

> **Prefixo de todos os endpoints:** `/api/v4`
>
> Exemplo: `GET http://localhost:8082/api/v4/requisicoes-compra`

---

## 1. O que este serviço faz

O Supplies é o serviço de **suprimentos e inventário** do Mekano. Ele é responsável por:

- **Estoque** de peças/insumos:
  - saldo atual;
  - saldo reservado;
  - estoque mínimo;
  - movimentos (`CREDITO`, `DEBITO`, `RESERVA`, `LIBERACAO`, `CONSUMO`);
  - consulta de peças abaixo do mínimo.
- **Requisição de compra**:
  - criar;
  - enviar;
  - receber;
  - cancelar;
  - listar;
  - buscar.
- **NF de entrada**:
  - registrar;
  - listar;
  - buscar;
  - cancelar (com estorno de estoque).
- **API interna de estoque** para consumidores futuros.

### Sprint 2 — estoque e ciclo da requisição

- Movimentos `CREDITO`, `DEBITO`, `RESERVA`, `LIBERACAO` e `CONSUMO` registrados
  em ledger com chave idempotente.
- Débitos serializados por lock pessimista na linha do estoque.
- A criação da requisição inicializa linhas de estoque; recebimento não credita
  saldo (crédito é responsabilidade da NF de entrada na Sprint 3).
- Contratos: [`docs/04-contratos-api.md`](docs/04-contratos-api.md).

## 2. O que este serviço NÃO faz

- Cadastro de clientes, veículos, peças (catálogo) e serviços — pertence ao `Mekano-Registry-Service-API`;
- Ordens de serviço, orçamentos, pagamentos, WhatsApp e autenticação.

---

## 3. Mudança de escopo: estoque saiu do Registry e veio para o Supplies

No monólito, o saldo ficava na entidade `Peca`. Nesta fase, a decisão foi separar:

- **Registry** = catálogo da peça (`codigo`, `descricao`, `valorUnitario`);
- **Supplies** = estoque (`saldo_atual`, `saldo_reservado`, `estoque_minimo`) e movimentos.

Motivos principais:

1. A NF de entrada precisa **creditar estoque na mesma transação** em que é salva. Com o estoque no Supplies, isso é **ACID local**, sem transação distribuída, sem flag `estoque_creditado` e sem endpoint de reprocessamento.
2. O cancelamento da NF precisa **estornar estoque localmente**, com rollback simples.
3. A reposição por estoque mínimo e a requisição de compra ficam no mesmo serviço.
4. O Registry permanece um CRUD de catálogo, sem acoplamento com o fluxo de compras.

Consequências:

- O Supplies referencia a peça por `peca_uuid` (sem FK cross-database).
- O Supplies cria a linha de estoque sob demanda (`obterOuCriar`).
- O catálogo é validado via `POST /api/v4/internal/pecas/batch` no Registry.
- O seed do Registry cria o catálogo; o seed do Supplies cria o estoque inicial.

Explicação completa em [`docs/adr/ADR-003-ownership-estoque.md`](docs/adr/ADR-003-ownership-estoque.md).

---

## 4. Stack

| Item | Versão/uso |
|---|---|
| Java | 17 |
| Quarkus | 3.36.0 |
| Maven | Multi-módulo (wrapper) |
| PostgreSQL | 16 |
| Flyway | Migrations |
| Hibernate ORM / Panache | Persistência |
| MapStruct | Mapeamento |
| Lombok | Redução de boilerplate |
| SmallRye OpenAPI | Swagger/OpenAPI |
| MicroProfile REST Client | Integração com o Registry |
| JUnit 5 / AssertJ / Mockito / RestAssured / WireMock | Testes |

## 5. Arquitetura

```text
supplies-rest            (Quarkus app / resources / DTOs)
  ├── supplies-application (services / use cases)
  ├── supplies-infrastructure (JPA / repos / mappers / migrations / client)
  └── supplies-domain (models / VOs / ports / exceptions)
```

- Pacotes base: `com.fiap.mekano.supplies.*`
- Prefixo REST: `quarkus.rest.path=/api/v4`
- Banco: `mekano_supplies`
- Porta da API: `8082`
- Porta do PostgreSQL: `5434`
- Dependência externa: Registry em `http://localhost:8081/api/v4`

Detalhes em [`docs/01-arquitetura.md`](docs/01-arquitetura.md).

---

## 6. Como rodar localmente

### 6.1 Pré-requisitos

- Java 17
- Docker Desktop
- Maven Wrapper (`./mvnw` / `mvnw.cmd`)
- Registry rodando (para validação de peças)

### 6.2 Subir o Registry primeiro

```bash
cd ../Mekano-Registry-Service-API
docker compose up -d --build
```

### 6.3 Subir o Supplies

```bash
docker compose up -d --build
```

A API ficará disponível em:

- API: `http://localhost:8082/api/v4`
- Swagger UI: `http://localhost:8082/q/swagger-ui`
- Health: `http://localhost:8082/q/health`
- Métricas: `http://localhost:8082/q/metrics`

### 6.4 Rodar em modo dev (Maven)

```bash
docker compose up -d supplies-db
./mvnw quarkus:dev -pl supplies-rest -am
```

Windows:

```powershell
docker compose up -d supplies-db
.\mvnw.cmd quarkus:dev -pl supplies-rest -am
```

### 6.5 Resetar banco e reaplicar migrations/seed

```bash
docker compose down -v
docker compose up -d --build
```

> As migrations são **finais** (sem migrations de correção). Durante a fase acadêmica, alterações de schema devem ser feitas na migration original e o volume recriado.

### 6.6 Testes automatizados

Os testes usam **H2 em memória (modo PostgreSQL)**, sem depender de Docker:

```bash
./mvnw -B -ntp verify
```

- `MigrationSmokeTest` aplica `V1`/`V2` do zero via Flyway.
- Testes de contrato do client do Registry usam **WireMock**.
- O cenário E2E com os serviços reais está documentado no README do Registry e pode ser executado pelo script `scripts/Test-RegistrySuppliesE2E.ps1` do repositório Registry.
- Testes REST usam `@InjectMock` do `PecaCatalogPort` e RestAssured.

---

## 7. Banco de dados e DBeaver

Após subir os containers:

| Configuração | Valor |
|---|---|
| Driver | PostgreSQL |
| Host | `localhost` |
| Porta | `5434` |
| Database | `mekano_supplies` |
| Usuário | `mekano` |
| Senha | `mekano` |

Passo a passo no DBeaver:

1. **Database → New Database Connection → PostgreSQL**.
2. Preencha host, porta, database, usuário e senha conforme a tabela.
3. Teste a conexão e finalize.
4. As tabelas estarão no schema `public`.

Detalhes em [`docs/06-operacao-local.md`](docs/06-operacao-local.md).

---

## 8. Seed inicial

> **Previsto para o Sprint 2/3.** A migration `V6__seed_estoque_inicial.sql` ainda
> não existe nesta branch (Sprint 1 cobre apenas `V1` e `V2`). Quando implementada,
> criará o estoque das peças do seed do Registry:

| Peça | UUID | saldo_atual | saldo_reservado | estoque_minimo |
|---|---|---|---|---|
| `PEA-001` | `55555555-5555-5555-5555-555555555555` | 10 | 0 | 2 |
| `PEA-002` | `66666666-6666-6666-6666-666666666666` | 5 | 0 | 3 |

> O catálogo das peças é criado pelo Registry (`V5__seed_dados_iniciais.sql`). O Supplies cria apenas o estoque correspondente.

---

## 9. Endpoints

### 9.1 Implementados (Sprint 1)

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/v4/requisicoes-compra` | Cria requisição `ABERTA` com `motivo=ESTOQUE_MINIMO` |
| GET | `/api/v4/requisicoes-compra` | Lista com filtros (`status`, `motivo`, `pecaUuid`) e paginação |
| GET | `/api/v4/requisicoes-compra/{uuid}` | Busca por UUID |

### 9.2 Previstos (Sprints 2 e 3)

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/v4/requisicoes-compra/{uuid}/enviar` | `ABERTA → ENVIADA` |
| POST | `/api/v4/requisicoes-compra/{uuid}/receber` | `ENVIADA`/`COMPRA_APROVADA → PRODUTO_RECEBIDO` |
| POST | `/api/v4/requisicoes-compra/{uuid}/cancelar` | `ABERTA → CANCELADA` |
| POST | `/api/v4/nf-entrada` | Registra NF `ATIVA` + credita estoque |
| GET | `/api/v4/nf-entrada` | Lista com filtros |
| GET | `/api/v4/nf-entrada/{uuid}` | Busca NF + itens + movimentos |
| POST | `/api/v4/nf-entrada/{uuid}/cancelar` | `ATIVA → CANCELADA` + estorna estoque |

### 9.3 Internos (estoque — Sprint 2)

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/v4/internal/estoque/movimentos` | Movimenta estoque com idempotência |
| GET | `/api/v4/internal/estoque/{pecaUuid}` | Consulta saldo |
| GET | `/api/v4/internal/estoque/abaixo-minimo` | Lista peças abaixo do mínimo |

Contratos completos em [`docs/04-contratos-api.md`](docs/04-contratos-api.md).

---

## 10. Roles (futuras, comentadas)

A autenticação está **fora do escopo** desta fase. Os endpoints terão `@RolesAllowed` comentado com `TODO`:

| Endpoint | Roles propostas |
|---|---|
| Requisições de compra | `admin`, `financeiro` |
| NF de entrada | `admin` |
| Estoque interno | `service:estoque` |

Detalhes em [`docs/07-decisoes-e-riscos.md`](docs/07-decisoes-e-riscos.md).

---

## 11. Testes e Postman

- Testes unitários, de integração, REST, contrato e concorrência.
- **Sprint 1 (atual):**
  - domínio (`RequisicaoCompraTest`, `ItemRequisicaoCompraTest`);
  - application (`RequisicaoCompraServiceTest`, mock do `PecaCatalogPort`);
  - repositório (`RequisicaoCompraRepositoryImplTest`);
  - REST (`RequisicaoCompraResourceTest`, health check);
  - contrato do client do Registry (`RegistryPecaCatalogAdapterTest` + WireMock);
  - migrations (`MigrationSmokeTest`).
- A **migração dos testes automatizados** das pastas `/test` do monólito e a **organização dos JSONs do Postman** serão feitas na branch final:
  - `test/sprint-5-testes-e-postman`
- Durante os sprints 1–4, cada PR mantém o build verde com testes mínimos das novas classes.
- Estratégia completa em [`docs/05-testes-e-postman.md`](docs/05-testes-e-postman.md).

---

## 12. Branches e fluxo de entrega

| Branch | Sprint | Objetivo |
|---|---|---|
| `start` | Sprint 0 | Estrutura inicial, docs, README, `.gitignore`, POM, compose e CI |
| `feature/sprint-1-requisicoes-compra` | Sprint 1 | Requisição de compra (criar/listar/buscar) |
| `feature/sprint-2-estoque-ciclo-requisicao` | Sprint 2 | Estoque, movimentos e ciclo da requisição |
| `feature/sprint-3-nf-entrada` | Sprint 3 | NF de entrada e cancelamento com estorno |
| `feature/sprint-4-integracao-entrega` | Sprint 4 | Integração, hardening e release |
| `test/sprint-5-testes-e-postman` | Sprint 5 | Migração/organização dos testes e Postman |

Regras:

- **Nunca** push direto em `main`.
- Cada sprint = uma branch + PR para `main`.
- Exceção única: commit inicial vazio em `main` para viabilizar o primeiro PR.
- Merge somente com CI verde.

Plano detalhado em [`docs/02-plano-de-tarefas.md`](docs/02-plano-de-tarefas.md).

---

## 13. Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/00-escopo.md`](docs/00-escopo.md) | Escopo, limites e mudança de escopo do estoque |
| [`docs/01-arquitetura.md`](docs/01-arquitetura.md) | Clean Architecture, módulos, dependências e banco |
| [`docs/02-plano-de-tarefas.md`](docs/02-plano-de-tarefas.md) | Sprints, branches, tarefas e critérios de aceite |
| [`docs/03-modelo-de-dados.md`](docs/03-modelo-de-dados.md) | Tabelas, migrations, seed e regras de estoque |
| [`docs/04-contratos-api.md`](docs/04-contratos-api.md) | Endpoints públicos e internos |
| [`docs/05-testes-e-postman.md`](docs/05-testes-e-postman.md) | Migração de testes e Postman |
| [`docs/06-operacao-local.md`](docs/06-operacao-local.md) | Docker, DBeaver, variáveis e troubleshooting |
| [`docs/07-decisoes-e-riscos.md`](docs/07-decisoes-e-riscos.md) | ADRs, decisões e riscos |
| [`docs/adr/ADR-003-ownership-estoque.md`](docs/adr/ADR-003-ownership-estoque.md) | Explicação completa da mudança de escopo do estoque |
| [`docs/adr/ADR-005-idempotencia-estoque.md`](docs/adr/ADR-005-idempotencia-estoque.md) | Idempotência e consistência do estoque |

---

## 14. Licença

Projeto acadêmico — FIAP. Uso educacional.
