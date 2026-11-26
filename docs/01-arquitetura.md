# Arquitetura — Mekano Supplies Purchase Request Service API

## 1. Visão geral

O Supplies é um serviço Quarkus independente, com banco próprio, seguindo Clean Architecture e os padrões do monólito Mekano.

```text
┌─────────────────────────────────────────────────────┐
│                Supplies Service                     │
│                                                     │
│  supplies-rest (Quarkus / JAX-RS / DTOs)            │
│        │                                            │
│  supplies-application (use cases)                   │
│        │                                            │
│  supplies-domain (models/VOs/ports)                 │
│        ▲                                            │
│  supplies-infrastructure (JPA/repos/Flyway/client)  │
│        │                                            │
│  PostgreSQL 16 — mekano_supplies                    │
└─────────────────────────────────────────────────────┘
                 │
                 │ POST /api/v4/internal/pecas/batch
                 ▼
┌─────────────────────────────────────────────────────┐
│              Registry Service                       │
│         PostgreSQL 16 — mekano_registry             │
└─────────────────────────────────────────────────────┘
```

O Supplies **depende** do Registry apenas para consultar o catálogo de peças. O Registry **não depende** do Supplies.

---

## 2. Módulos Maven

| Módulo | Responsabilidade | Dependências |
|---|---|---|
| `supplies-domain` | Entidades, VOs, enums, ports, exceções | Java SE + Lombok |
| `supplies-application` | Services/use cases, commands, orquestração | `supplies-domain`, `quarkus-arc` |
| `supplies-infrastructure` | Entities JPA, repositórios, mappers, migrations, auditoria, client REST | `supplies-domain`, Quarkus/Hibernate/Flyway/REST Client |
| `supplies-rest` | Resources, DTOs, exception mapper, config Quarkus | `supplies-application`, `supplies-infrastructure` |

Regras:

- `@Transactional` nos services (nunca em resources/repositórios).
- Resources com `@RequestScoped`.
- DTOs de entrada como classes com Bean Validation; DTOs de saída como `record`.
- Mapeamento com MapStruct (`componentModel = "cdi"`).
- Jandex nos módulos necessários.
- Quarkus Maven Plugin apenas no `supplies-rest`.

---

## 3. Pacotes

```text
com.fiap.mekano.supplies
├── domain
│   ├── model
│   ├── valueobject
│   ├── port/in
│   ├── port/out
│   └── exception
├── application
│   └── service
├── infrastructure
│   ├── entity
│   ├── repository
│   ├── mapper
│   ├── audit
│   ├── client
│   └── config
└── rest
    ├── api
    ├── dto
    └── mapper
```

---

## 4. Stack e configuração

| Item | Valor |
|---|---|
| Java | 17 |
| Quarkus | 3.36.0 |
| REST prefix | `/api/v4` |
| Banco | PostgreSQL 16 |
| Database | `mekano_supplies` |
| Porta API | `8082` |
| Porta DB | `5434` |
| Migrations | Flyway |
| Documentação | SmallRye OpenAPI |
| Health/Metrics | SmallRye Health / Micrometer |
| Cliente Registry | MicroProfile REST Client |

Configuração principal:

```properties
quarkus.rest.path=/api/v4
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5434/mekano_supplies
quarkus.datasource.username=mekano
quarkus.datasource.password=mekano
quarkus.flyway.migrate-at-start=true

registry.base-url=http://localhost:8081
registry.timeout-ms=3000
```

---

## 5. Dependências entre serviços

```text
Supplies ──POST /api/v4/internal/pecas/batch──► Registry
Registry ──nenhuma chamada──► Supplies
```

- Comunicação síncrona REST/JSON.
- Sem broker, sem eventos, sem saga.
- Sem acesso ao banco do Registry.
- Timeout configurável no cliente.
- Retry apenas em chamadas idempotentes (GET/batch).
- Versionamento via `/api/v4`.

---

## 6. Ownership do estoque

O estoque é **local ao Supplies**:

- `estoque` e `movimentos_estoque` no banco `mekano_supplies`.
- NF e estoque na mesma transação local.
- Cancelamento de NF com estorno local.
- Nenhuma chamada remota para creditar/estornar estoque.
- O Registry é apenas catálogo.

Detalhes em [`adr/ADR-003-ownership-estoque.md`](adr/ADR-003-ownership-estoque.md).

---

## 7. Camadas e responsabilidades

### Domain

- Entidades imutáveis com factories `create`/`reconstitute`.
- VOs imutáveis com validação no construtor.
- Ports de entrada e saída sem anotações de framework.
- Exceção única `AppException` com status HTTP.
- Mensagens via `Messages` (ResourceBundle).

### Application

- Services `@ApplicationScoped` com injeção por construtor.
- `@Transactional` nos métodos de escrita.
- Validações de negócio e orquestração.
- Logs com `io.quarkus.logging.Log` (INFO/WARN; ERROR só no mapper).
- Sem dependência de JAX-RS/JPA.

### Infrastructure

- Entities JPA com `BaseEntity` e auditoria.
- Repositórios Panache.
- Mappers MapStruct.
- Migrations Flyway em `src/main/resources/db/migration`.
- Cliente REST do Registry.
- Soft delete e índices únicos parciais.
- Controle de concorrência no estoque.

### REST

- Resources `@RequestScoped`.
- DTOs de entrada/saída.
- Bean Validation.
- `ApiExceptionMapper` com RFC 7807 (`application/problem+json`).
- Paginação padrão.
- `@RolesAllowed` comentado com `TODO`.

---

## 8. Banco e migrations

- Uma migration final por tabela:
  - `V1__create_requisicoes_compra.sql`
  - `V2__create_itens_requisicao_compra.sql`
  - `V3__create_estoque.sql`
  - `V4__create_movimentos_estoque.sql`
  - `V5__create_nf_entradas.sql`
  - `V6__seed_estoque_inicial.sql`
- Sem migrations de correção nesta fase.
- `BIGINT GENERATED BY DEFAULT AS IDENTITY`.
- `UUID` único exposto na API.
- Soft delete e auditoria.
- Sem FK cross-database para `peca_uuid`.

Detalhes em [`03-modelo-de-dados.md`](03-modelo-de-dados.md).

---

## 9. Consistência e concorrência

- Movimentos de estoque em transação local.
- Lock pessimista ou `UPDATE` atômico com verificação de saldo.
- `idempotency_key` única no ledger.
- Duplicata de chave retorna o movimento existente.
- NF + estoque: transação única.
- Cancelamento de NF + estorno: transação única.

Detalhes em [`adr/ADR-005-idempotencia-estoque.md`](adr/ADR-005-idempotencia-estoque.md).

---

## 10. Observabilidade

- `/q/health` e `/q/health/ready`.
- `/q/metrics`.
- Logs JSON em produção/dev.
- Correlation ID (se o padrão do monólito for mantido).
- Sem PII nos logs.
- Logs de movimentos com `pecaUuid`, `tipo`, `quantidade` e `referenciaUuid` (sem chave de NF completa).

---

## 11. Decisões arquiteturais relacionadas

- [`adr/ADR-003-ownership-estoque.md`](adr/ADR-003-ownership-estoque.md): estoque no Supplies, catálogo no Registry.
- [`adr/ADR-005-idempotencia-estoque.md`](adr/ADR-005-idempotencia-estoque.md): idempotência, ledger e transação local.
- [`07-decisoes-e-riscos.md`](07-decisoes-e-riscos.md): stack, PostgreSQL, REST, migrations finais, NF.
