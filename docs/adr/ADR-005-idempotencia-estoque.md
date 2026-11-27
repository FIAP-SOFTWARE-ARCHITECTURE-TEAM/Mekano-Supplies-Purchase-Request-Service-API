# ADR-005 — Idempotência e consistência do estoque

**Status:** Aceita
**Fase:** 2
**Data:** 2026-11-26

---

## 1. Contexto

O estoque é um recurso compartilhado e sensível a concorrência. No Supplies, ele é alterado por:

- NF de entrada (crédito);
- cancelamento de NF (estorno);
- requisição de compra (criação da linha de estoque);
- consumidores futuros (reserva, liberação, consumo).

A NF precisa ser registrada e creditar estoque de forma consistente. O cancelamento precisa estornar sem deixar saldo negativo.

---

## 2. Decisão

### 2.1 NF em transação local

Registrar NF e creditar estoque ocorrem na **mesma transação local** do banco `mekano_supplies`:

```text
BEGIN
  valida requisição PRODUTO_RECEBIDO
  valida chave 44 dígitos única
  valida valor > 0
  INSERT nf_entradas (status=ATIVA)
  para cada item da requisição:
    UPDATE estoque SET saldo_atual = saldo_atual + qtd
    INSERT movimentos_estoque (CREDITO)
COMMIT
```

- Falha em qualquer etapa → rollback total.
- **Não** existe flag `estoque_creditado`.
- **Não** existe endpoint de reprocessamento.

### 2.2 Cancelamento em transação local

```text
BEGIN
  valida NF ATIVA
  para cada item da requisição:
    UPDATE estoque SET saldo_atual = saldo_atual - qtd
      WHERE saldo_atual >= qtd
    INSERT movimentos_estoque (DEBITO)
  UPDATE nf_entradas SET status=CANCELADA, ...
COMMIT
```

- Saldo insuficiente → `409` + rollback; NF permanece `ATIVA`.

### 2.3 Idempotência no ledger

Cada movimento tem `idempotency_key` única:

```text
nf:<nfUuid>:<pecaUuid>:CREDITO
nf:<nfUuid>:<pecaUuid>:ESTORNO
```

- Duplicata → retorna o movimento existente, sem alterar saldo.
- A resposta inclui `idempotente=true`.
- A idempotência é útil para chamadas remotas futuras e para retries seguros.

### 2.4 Concorrência

- Lock pessimista na linha de estoque (`SELECT ... FOR UPDATE`) **ou**
- `UPDATE` atômico com verificação de saldo:

```sql
UPDATE estoque
SET saldo_atual = saldo_atual - :qtd,
    updated_at = NOW()
WHERE peca_uuid = :pecaUuid
  AND saldo_atual >= :qtd;
```

- Se `rowsAffected = 0` → `409` (saldo insuficiente).
- `version` disponível para concorrência otimista, se necessário.

---

## 3. Consequências

### Positivas

- Consistência forte entre NF e estoque.
- Sem transação distribuída.
- Sem flag/reprocessamento.
- Retries seguros.
- Sem lost update em operações concorrentes.

### Negativas / custos

- A transação da NF pode ficar um pouco mais longa (salvar NF + N movimentos).
- Necessidade de testes de concorrência.
- Necessidade de definir `idempotency_key` consistente.

### Mitigações

- Transação curta e local.
- Índices em `peca_uuid` e `idempotency_key`.
- Testes paralelos de débito/reserva.
- Logs sem PII.

---

## 4. Alternativas consideradas

### Alternativa A — Estoque no Registry com idempotência + flag (rejeitada)

- NF no Supplies chamaria o Registry para creditar.
- Flag `estoque_creditado` + endpoint de reprocessamento.
- **Motivo da rejeição:** transação distribuída e maior complexidade.

### Alternativa B — Estoque no Supplies com transação local (aceita)

- NF e estoque no mesmo banco.
- Idempotência apenas no ledger.
- **Motivo da escolha:** consistência e simplicidade.

---

## 5. Regras de saldo

| Tipo | Efeito | Validação |
|---|---|---|
| `CREDITO` | `saldo_atual += qtd` | quantidade > 0 |
| `DEBITO` | `saldo_atual -= qtd` | `saldo_atual >= qtd` |
| `RESERVA` | `saldo_reservado += qtd` | quantidade > 0 |
| `LIBERACAO` | `saldo_reservado -= qtd` | `saldo_reservado >= qtd` |
| `CONSUMO` | `saldo_atual -= qtd` e `saldo_reservado -= qtd` | ambos >= qtd |

- `disponivel = saldo_atual - saldo_reservado`.
- `saldo_reservado` pode ser maior que `saldo_atual` (sinaliza falta).
- Estoque abaixo do mínimo: `estoque_minimo IS NOT NULL AND disponivel <= estoque_minimo`.

---

## 6. Referências

- [`ADR-003-ownership-estoque.md`](ADR-003-ownership-estoque.md)
- [`../03-modelo-de-dados.md`](../03-modelo-de-dados.md)
- [`../04-contratos-api.md`](../04-contratos-api.md)
