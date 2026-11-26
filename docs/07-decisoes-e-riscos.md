# Decisões e riscos — Mekano Supplies Purchase Request Service API

---

## 1. Decisões (ADRs)

| ADR | Decisão | Motivo |
|---|---|---|
| ADR-001 | Java 17 + Quarkus 3.36.0 + Maven multi-módulo | Mesmo stack do monólito; produtividade e consistência |
| ADR-002 | PostgreSQL 16 | Estoque exige ACID, constraints, locks e transações multi-tabela |
| ADR-003 | Estoque no Supplies; Registry = catálogo | NF + estoque em transação local; sem transação distribuída |
| ADR-004 | REST/JSON síncrono | Simplicidade; sem broker nesta fase |
| ADR-005 | Idempotência no ledger de estoque; NF local ACID | Retries seguros e consistência local |
| ADR-006 | Migrations finais + seed | Não há dados a migrar; projeto acadêmico reiniciado |
| ADR-007 | Sem flag `estoque_creditado` e sem reprocessamento | A transação local torna o flag desnecessário |
| ADR-008 | `/api/v4` | Fase 4 do projeto |

---

## 2. Roles futuras (comentadas)

A autenticação está fora do escopo. Todos os endpoints terão `@RolesAllowed` comentado com `TODO`.

### Requisições de compra

| Operação | Roles propostas |
|---|---|
| `POST`, `GET`, `enviar`, `receber`, `cancelar` | `admin`, `financeiro` |

### NF de entrada

| Operação | Roles propostas |
|---|---|
| `POST`, `GET`, `cancelar` | `admin` |

### Estoque interno

| Operação | Roles propostas |
|---|---|
| `POST /api/v4/internal/estoque/movimentos` | `service:estoque` |
| `GET /api/v4/internal/estoque/**` | `service:estoque` |

Formato:

```java
// TODO: habilitar quando autenticação entrar no escopo
// @RolesAllowed({"admin", "financeiro"})
```

---

## 3. Mudança de escopo do estoque

O estoque **saiu do Registry** e veio para o Supplies.

### 3.1 O que mudou

| Antes (monólito) | Depois (Fase 4) |
|---|---|
| `Peca` com `saldoAtual`, `saldoReservado`, `estoqueMinimo` | `Peca` no Registry apenas com catálogo |
| NF chamava `creditarSaldo` na `Peca` | NF credita estoque local no Supplies |
| Estoque e catálogo no mesmo agregado | Catálogo no Registry; estoque no Supplies |
| Uma transação local no monólito | Uma transação local no Supplies para NF + estoque |

### 3.2 Por que

1. **ACID local na NF**: salvar NF, creditar estoque e gravar movimentos no mesmo banco.
2. **Cancelamento com estorno local**: sem compensação remota.
3. **Reposição e requisição no mesmo serviço**: estoque mínimo + compras coesos.
4. **Registry limpo**: apenas catálogo, sem acoplamento com compras.
5. **Sem ciclo**: Supplies → Registry (batch); Registry não chama Supplies.

### 3.3 Consequências

- `peca_uuid` é referência lógica, sem FK cross-database.
- `obterOuCriar` cria a linha de estoque sob demanda.
- Snapshots de código/descrição/valor no item da requisição.
- Seed dividido: catálogo no Registry, estoque no Supplies.
- Peça inativa: batch retorna `isActive`; Supplies bloqueia novas operações.

Explicação completa em [`adr/ADR-003-ownership-estoque.md`](adr/ADR-003-ownership-estoque.md).

---

## 4. Consistência e idempotência

- `movimentos_estoque.idempotency_key` é única.
- Duplicata retorna o movimento existente, sem alterar saldo.
- NF + estoque: transação local única.
- Cancelamento + estorno: transação local única.
- Concorrência: lock pessimista ou `UPDATE` atômico com verificação de saldo.
- **Sem** flag `estoque_creditado`.
- **Sem** endpoint de reprocessamento de estoque da NF.

Detalhes em [`adr/ADR-005-idempotencia-estoque.md`](adr/ADR-005-idempotencia-estoque.md).

---

## 5. Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Peça inativada no Registry | Requisições/NF com peça inativa | Batch retorna `isActive`; bloquear novas operações e preservar histórico |
| Divergência de UUID entre serviços | Estoque órfão | UUIDs fixos no seed + contract tests + E2E |
| Concorrência no saldo | Lost update / saldo negativo | Lock pessimista ou `UPDATE` atômico + testes paralelos |
| Duplicidade de movimento | Estoque creditado/debitado duas vezes | `idempotency_key` única |
| Cancelamento de NF sem saldo | Estoque inconsistente | `409` + rollback; NF permanece `ATIVA` |
| Registry indisponível | Criação de requisição falha | `503`; NF continua local e independente |
| Migration alterada após aplicação | Flyway falha | Reset de volume em dev; migrations finais |
| Contrato do batch mudar | Quebra a integração | OpenAPI + contract tests + `/api/v4` |
| Escopo crescer | Atraso | OS/orçamento/pagamento/WhatsApp/auth fora do escopo |

---

## 6. Pontos de atenção

- Não acessar o banco do Registry.
- Não criar FK cross-database para `peca_uuid`.
- Não creditar/estornar estoque via chamada remota.
- Não reintroduzir `estoque_creditado` ou reprocessamento.
- Não commitar `.env` com credenciais.
- Não fazer push direto em `main`.
- Manter `/api/v4` em todos os endpoints, testes e Postman.

---

## 7. ADRs relacionados

- [`adr/ADR-003-ownership-estoque.md`](adr/ADR-003-ownership-estoque.md)
- [`adr/ADR-005-idempotencia-estoque.md`](adr/ADR-005-idempotencia-estoque.md)
- [`05-testes-e-postman.md`](05-testes-e-postman.md)
- [`03-modelo-de-dados.md`](03-modelo-de-dados.md)
