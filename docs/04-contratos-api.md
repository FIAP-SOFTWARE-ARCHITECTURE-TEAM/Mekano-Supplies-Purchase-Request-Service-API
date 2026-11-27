# Contratos de API — Mekano Supplies Purchase Request Service API

**Prefixo:** `/api/v4`
**Base local:** `http://localhost:8082/api/v4`
**Registry:** `http://localhost:8081/api/v4`

> **Sprint 2:** implementados os endpoints de requisição, o ciclo de vida e a API
> interna de estoque descrita neste documento. NF de entrada pertence à Sprint 3.

---

## 1. Convenções

- JSON em UTF-8.
- Datas em ISO-8601.
- Valores monetários como número decimal.
- Paginação:
  - query `page` (default `0`) e `size` (default `20`, máximo `100`);
  - response com `content`, `page`, `size`, `totalElements`, `totalPages`.
- Erros no padrão **RFC 7807** (`application/problem+json`).
- `@RolesAllowed` comentado com `TODO` em todos os endpoints.

---

## 2. Requisição de compra

### 2.1 Criar

```http
POST /api/v4/requisicoes-compra
Content-Type: application/json

{
  "motivo": "ESTOQUE_MINIMO",
  "observacao": "Reposição de estoque",
  "itens": [
    { "pecaUuid": "55555555-5555-5555-5555-555555555555", "quantidade": 10 }
  ]
}
```

Resposta `201`:

```json
{
  "uuid": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "status": "ABERTA",
  "motivo": "ESTOQUE_MINIMO",
  "observacao": "Reposição de estoque",
  "itens": [
    {
      "pecaUuid": "55555555-5555-5555-5555-555555555555",
      "pecaCodigo": "PEA-001",
      "pecaDescricao": "Filtro de Óleo",
      "quantidade": 10,
      "valorUnitario": 45.90
    }
  ],
  "createdAt": "2026-11-26T10:00:00"
}
```

Regras:

- Pelo menos 1 item.
- Sem peça duplicada.
- Quantidade > 0.
- `motivo` público: `ESTOQUE_MINIMO`.
- `ORDEM_SERVICO` reservado para criação interna futura.
- Peças validadas via Registry:
  - não encontrada → `404`;
  - inativa → `409`.
- Snapshots de código, descrição e valor são gravados.
- Linha de estoque criada com saldo 0 se não existir.

### 2.2 Listar

```http
GET /api/v4/requisicoes-compra?page=0&size=20&status=ABERTA&motivo=ESTOQUE_MINIMO&pecaUuid=...
```

### 2.3 Buscar por UUID

```http
GET /api/v4/requisicoes-compra/{uuid}
```

### 2.4 Enviar

```http
POST /api/v4/requisicoes-compra/{uuid}/enviar
```

- `ABERTA → ENVIADA`.
- Transição inválida → `409`.

### 2.5 Receber

```http
POST /api/v4/requisicoes-compra/{uuid}/receber
```

- `ENVIADA` ou `COMPRA_APROVADA → PRODUTO_RECEBIDO`.
- Transição inválida → `409`.

### 2.6 Cancelar

```http
POST /api/v4/requisicoes-compra/{uuid}/cancelar
```

- `ABERTA → CANCELADA`.
- `motivo=ORDEM_SERVICO` → `409`.
- Demais status → `409`.

### 2.7 Roles comentadas

```java
// TODO: habilitar quando autenticação entrar no escopo
// @RolesAllowed({"admin", "financeiro"})
```

---

## 3. NF de entrada

### 3.1 Registrar

```http
POST /api/v4/nf-entrada
Content-Type: application/json

{
  "requisicaoUuid": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "chaveAcesso": "12345678901234567890123456789012345678901234",
  "valorTotal": 459.00
}
```

Resposta `201`:

```json
{
  "uuid": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "requisicaoUuid": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "chaveAcesso": "12345678901234567890123456789012345678901234",
  "valorTotal": 459.00,
  "status": "ATIVA",
  "motivoCancelamento": null,
  "dataCancelamento": null,
  "createdAt": "2026-11-26T10:00:00",
  "itens": [
    {
      "pecaUuid": "55555555-5555-5555-5555-555555555555",
      "pecaCodigo": "PEA-001",
      "pecaDescricao": "Filtro de Oleo",
      "quantidade": 10,
      "valorUnitario": 45.90
    }
  ],
  "estoque": [
    {
      "pecaUuid": "55555555-5555-5555-5555-555555555555",
      "quantidade": 10,
      "saldoAtualApos": 20
    }
  ]
}
```

Regras:

- Requisição precisa estar `PRODUTO_RECEBIDO` → senão `409`.
- Chave com 44 dígitos e única → senão `400`/`409`.
- Valor > 0 → senão `400`.
- Itens da NF = itens da requisição.
- **Transação local**:
  - salva NF `ATIVA`;
  - credita estoque;
  - grava movimentos.
- Falha → rollback total.
- **Sem** flag `estoque_creditado` e **sem** endpoint de reprocessamento.

### 3.2 Listar

```http
GET /api/v4/nf-entrada?page=0&size=20&status=ATIVA&requisicaoUuid=...&chaveAcesso=...
```

Resposta `200` com `content`, `page`, `size`, `totalElements` e `totalPages`.
Filtro `status` inválido (diferente de `ATIVA`/`CANCELADA`) retorna `400`.

### 3.3 Buscar por UUID

```http
GET /api/v4/nf-entrada/{uuid}
```

Retorna NF, itens da requisição e movimentos de estoque gerados.

### 3.4 Cancelar

```http
POST /api/v4/nf-entrada/{uuid}/cancelar
Content-Type: application/json

{
  "motivo": "Nota emitida em duplicidade"
}
```

Respostas: `200` com a NF cancelada e os movimentos de estorno; `400` para
motivo inválido; `404` para UUID inexistente; `409` para NF já cancelada ou
saldo insuficiente.

Regras:

- Somente NF `ATIVA` → senão `409`.
- **Transação local**:
  - debita estoque de cada item;
  - grava movimentos de estorno;
  - marca `CANCELADA`.
- Saldo insuficiente → `409` + rollback; NF permanece `ATIVA`.
- Chave de idempotência do estorno: `nf:<uuid>:<pecaUuid>:ESTORNO`.

### 3.5 Roles comentadas

```java
// TODO: habilitar quando autenticação entrar no escopo
// @RolesAllowed("admin")
```

---

## 4. API interna de estoque

### 4.1 Movimentar

```http
POST /api/v4/internal/estoque/movimentos
Content-Type: application/json

{
  "pecaUuid": "55555555-5555-5555-5555-555555555555",
  "tipo": "CREDITO",
  "quantidade": 10,
  "idempotencyKey": "nf:bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb:55555555-5555-5555-5555-555555555555:CREDITO",
  "origem": "NF_ENTRADA",
  "referenciaUuid": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
}
```

Resposta `201` para um movimento aplicado; `200` quando a chave idempotente já
foi usada pela mesma operação:

```json
{
  "uuid": "cccccccc-cccc-cccc-cccc-cccccccccccc",
  "pecaUuid": "55555555-5555-5555-5555-555555555555",
  "tipo": "CREDITO",
  "quantidade": 10,
  "saldoAtualApos": 20,
  "saldoReservadoApos": 0,
  "origem": "NF_ENTRADA",
  "referenciaUuid": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "idempotencyKey": "nf:bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb:55555555-5555-5555-5555-555555555555:CREDITO",
  "createdAt": "2026-11-27T10:00:00",
  "idempotente": false
}
```

Regras:

- Mesma `idempotencyKey` → `200` com `idempotente=true`, sem alterar saldo.
- `DEBITO`/`CONSUMO`/`LIBERACAO` sem saldo → `409`.
- `RESERVA` pode deixar `disponivel` negativo.
- `@RolesAllowed("service:estoque")` comentado.

### 4.2 Consultar saldo

```http
GET /api/v4/internal/estoque/{pecaUuid}
```

Resposta `200`:

```json
{
  "uuid": "dddddddd-dddd-dddd-dddd-dddddddddddd",
  "pecaUuid": "55555555-5555-5555-5555-555555555555",
  "saldoAtual": 20,
  "saldoReservado": 0,
  "disponivel": 20,
  "estoqueMinimo": 2,
  "version": 1,
  "createdAt": "2026-11-27T10:00:00",
  "updatedAt": "2026-11-27T10:05:00"
}
```

- Cria a linha com zeros se não existir; não cria movimento.

### 4.3 Abaixo do mínimo

```http
GET /api/v4/internal/estoque/abaixo-minimo
```

Resposta `200`:

```json
{
  "content": [
    {
      "uuid": "dddddddd-dddd-dddd-dddd-dddddddddddd",
      "pecaUuid": "55555555-5555-5555-5555-555555555555",
      "saldoAtual": 1,
      "saldoReservado": 0,
      "disponivel": 1,
      "estoqueMinimo": 2,
      "version": 4,
      "createdAt": "2026-11-27T10:00:00",
      "updatedAt": "2026-11-27T10:05:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Regra: `estoque_minimo IS NOT NULL AND disponivel <= estoque_minimo`.

---

## 5. Integração com o Registry

### 5.1 Batch de peças

O Supplies consome:

```http
POST http://localhost:8081/api/v4/internal/pecas/batch
Content-Type: application/json

{
  "pecaUuids": ["55555555-...", "66666666-..."]
}
```

Resposta:

```json
{
  "pecas": [
    {
      "uuid": "55555555-...",
      "codigo": "PEA-001",
      "descricao": "Filtro de Óleo",
      "valorUnitario": 45.90,
      "isActive": true
    }
  ],
  "naoEncontradas": []
}
```

Uso:

- validar peças na criação da requisição;
- obter snapshots de código, descrição e valor;
- decidir bloqueio de peças inativas.

### 5.2 Falhas do Registry

| Situação | Comportamento |
|---|---|
| Registry indisponível | `503` na criação da requisição |
| Peça não encontrada | `404` |
| Peça inativa | `409` |
| Timeout | `503` |

> A NF **não** depende do Registry para creditar/estornar estoque. O estoque é local.

---

## 6. Máquina de estados da requisição

```text
ABERTA ──enviar──► ENVIADA ──receber──► PRODUTO_RECEBIDO
   │
   └──cancelar──► CANCELADA

COMPRA_APROVADA ──receber──► PRODUTO_RECEBIDO   (estado reservado para futuro)
```

- `cancelar` só em `ABERTA`; bloqueia `motivo=ORDEM_SERVICO`.
- `receber` aceita `ENVIADA` e `COMPRA_APROVADA`.
- Demais transições → `409`.

---

## 7. Códigos de erro

| Status | Quando |
|---|---|
| `400` | Validação de entrada, chave inválida, valor <= 0 |
| `404` | Requisição/NF/peça não encontrada |
| `409` | Duplicidade, transição inválida, peça inativa, saldo insuficiente |
| `422` | Regra de negócio não atendida (quando aplicável) |
| `503` | Registry indisponível |
| `500` | Erro inesperado |

---

## 8. OpenAPI

- Swagger UI: `http://localhost:8082/q/swagger-ui`
- OpenAPI JSON: `http://localhost:8082/q/openapi`

Todos os schemas devem refletir o prefixo `/api/v4` e os DTOs reais.
