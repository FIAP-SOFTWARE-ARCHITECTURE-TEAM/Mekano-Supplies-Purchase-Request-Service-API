# ADR-003 — Ownership do estoque no Supplies

**Status:** Aceita
**Fase:** 4
**Data:** 2026-11-26

---

## 1. Contexto

No monólito Mekano, a entidade `Peca` acumulava:

1. **Catálogo:** código, descrição e valor unitário.
2. **Estoque:** saldo atual, saldo reservado, estoque mínimo e movimentos.

A NF de entrada chamava `creditarSaldo` na própria `Peca`, dentro do mesmo banco. Havia uma única transação local para salvar a NF e creditar o estoque.

Na nova arquitetura de microsserviços, o Registry foi definido como o serviço de **cadastros básicos** (clientes, veículos, peças e serviços) e o Supplies como o serviço de **requisição de compra e NF de entrada**.

O ponto crítico: se o estoque ficasse no Registry, a NF no Supplies precisaria de uma chamada remota para creditar/estornar estoque. Isso exigiria:

- idempotência + flag `estoque_creditado`;
- endpoint de reprocessamento em falha parcial;
- tratamento de inconsistência entre dois bancos.

Além disso, o estoque não é um campo de cadastro: é um **ledger transacional** com reservas, liberações, consumo e movimentos.

---

## 2. Decisão

**O estoque pertence ao Supplies. O Registry possui apenas o catálogo de peças.**

### Registry

- Tabela `pecas` com:
  - `uuid`
  - `codigo`
  - `descricao`
  - `valor_unitario`
  - `is_active`
  - auditoria
- **Sem** `saldo_atual`, `saldo_reservado` ou `estoque_minimo`.
- Endpoint interno `POST /api/v4/internal/pecas/batch`.

### Supplies

- Tabela `estoque`:
  - `peca_uuid` (referência lógica)
  - `saldo_atual`
  - `saldo_reservado`
  - `estoque_minimo`
  - `version`
- Tabela `movimentos_estoque`:
  - `tipo`
  - `quantidade`
  - `saldo_atual_apos`
  - `saldo_reservado_apos`
  - `origem`
  - `referencia_uuid`
  - `idempotency_key` única
- API interna de estoque em `/api/v4/internal/estoque/**`.
- NF de entrada e cancelamento em **transação local ACID**.

---

## 3. Consequências

### Positivas

1. **NF + estoque em uma única transação local**
   - Salvar NF, creditar estoque e gravar movimentos no mesmo banco.
   - Falha = rollback total.
   - Não é necessário flag `estoque_creditado`.
   - Não é necessário endpoint de reprocessamento.

2. **Cancelamento de NF com estorno local**
   - Estorno na mesma transação do cancelamento.
   - Saldo insuficiente → `409` + rollback; NF permanece `ATIVA`.

3. **Reposição e requisição de compra no mesmo serviço**
   - Estoque mínimo, movimentos e requisição coesos.
   - Criação de requisição por falta de estoque pode ser local no futuro.

4. **Registry permanece um CRUD de catálogo**
   - Sem acoplamento com compras.
   - Sem endpoints de estoque.

5. **Sem ciclo de dependência**
   - Supplies → Registry (batch).
   - Registry não chama Supplies.

6. **Ledger de estoque, NF e requisição no mesmo serviço**
   - Auditoria e rastreabilidade melhores.

### Negativas / custos

1. **Agregado “peça” dividido** entre catálogo e estoque.
2. **Referência lógica** por `peca_uuid`, sem FK cross-database.
3. **Criação sob demanda** da linha de estoque (`obterOuCriar`).
4. **Seed dividido**: catálogo no Registry, estoque no Supplies.
5. **Validação de peça** no Supplies via batch do Registry.

### Mitigações

- `obterOuCriar` cria a linha com saldo 0 na primeira referência.
- Snapshots de código/descrição/valor no item da requisição.
- Batch retorna `isActive`; o Supplies bloqueia novas operações para peças inativas.
- UUIDs fixos no seed e testes de contrato/E2E.
- Documentação explícita em ambos os repositórios.

---

## 4. Alternativas consideradas

### Alternativa A — Estoque no Registry (rejeitada)

- Registry teria `saldo_atual`, `saldo_reservado`, `estoque_minimo`.
- Supplies chamaria o Registry para creditar/estornar estoque na NF.
- Exigiria idempotência + flag `estoque_creditado` + endpoint de reprocessamento.
- **Motivo da rejeição:** transação distribuída, maior complexidade e risco de inconsistência parcial.

### Alternativa B — Estoque no Supplies (aceita)

- NF e estoque no mesmo banco.
- Transação local ACID.
- Idempotência mantida apenas no ledger para chamadas remotas futuras.
- **Motivo da escolha:** simplicidade, consistência e aderência ao domínio de suprimentos.

### Alternativa C — Serviço dedicado de estoque (futuro)

- Poderia ser extraído depois, se o volume ou o domínio justificarem.
- A API interna de estoque já isola o contrato, facilitando uma futura extração.

---

## 5. Impacto nos contratos

### Supplies

- `POST /api/v4/requisicoes-compra`
- `POST /api/v4/requisicoes-compra/{uuid}/enviar`
- `POST /api/v4/requisicoes-compra/{uuid}/receber`
- `POST /api/v4/requisicoes-compra/{uuid}/cancelar`
- `POST /api/v4/nf-entrada`
- `POST /api/v4/nf-entrada/{uuid}/cancelar`
- `POST /api/v4/internal/estoque/movimentos`
- `GET /api/v4/internal/estoque/{pecaUuid}`
- `GET /api/v4/internal/estoque/abaixo-minimo`

### Registry

- `POST /api/v4/internal/pecas/batch`
- Sem endpoints de estoque.

---

## 6. Migração de dados

Não há migração de dados:

- Projeto acadêmico reiniciado a cada fase.
- As migrations são escritas já no estado final.
- O seed do Registry cria o catálogo; o seed do Supplies cria o estoque inicial das mesmas peças.

---

## 7. Referências

- [`../00-escopo.md`](../00-escopo.md)
- [`../03-modelo-de-dados.md`](../03-modelo-de-dados.md)
- [`../04-contratos-api.md`](../04-contratos-api.md)
- Documentação do Registry: `Mekano-Registry-Service-API/docs/adr/ADR-003-catalogo-de-pecas.md`
