# Escopo — Mekano Supplies Purchase Request Service API

## 1. Objetivo

O Supplies é o serviço de **suprimentos e inventário** do Mekano. Ele centraliza o estoque das peças/insumos, as requisições de compra e as notas fiscais de entrada.

**Fase:** 4
**Prefixo REST:** `/api/v4`
**Banco:** `mekano_supplies` (PostgreSQL 16)
**Porta da API:** `8082`
**Porta do banco:** `5434`
**Dependência externa:** Registry (`POST /api/v4/internal/pecas/batch`)

---

## 2. Escopo incluído

### 2.1 Estoque

- Saldo atual, saldo reservado e estoque mínimo por peça.
- Movimentos:
  - `CREDITO`;
  - `DEBITO`;
  - `RESERVA`;
  - `LIBERACAO`;
  - `CONSUMO`.
- Ledger `movimentos_estoque` com `idempotency_key` única.
- Consulta de saldo por peça.
- Consulta de peças abaixo do estoque mínimo.
- Criação sob demanda da linha de estoque (`obterOuCriar`).
- Controle de concorrência com lock/atualização atômica.

### 2.2 Requisição de compra

- Criar requisição (`ABERTA`).
- Listar com filtros e paginação.
- Buscar por UUID.
- Enviar (`ABERTA → ENVIADA`).
- Receber (`ENVIADA`/`COMPRA_APROVADA → PRODUTO_RECEBIDO`).
- Cancelar (`ABERTA → CANCELADA`; bloqueia `ORDEM_SERVICO`).
- Validação de peças via batch do Registry.
- Snapshots de código, descrição e valor no item.

### 2.3 NF de entrada

- Registrar NF (`ATIVA`) e creditar estoque na **mesma transação local**.
- Listar com filtros e paginação.
- Buscar por UUID com itens da requisição e movimentos.
- Cancelar NF (`ATIVA → CANCELADA`) e estornar estoque na **mesma transação local**.
- Regras:
  - requisição precisa estar `PRODUTO_RECEBIDO`;
  - chave de acesso com 44 dígitos e única;
  - valor total > 0;
  - cancelamento com saldo insuficiente retorna `409` e rollback.

### 2.4 API interna de estoque

- `POST /api/v4/internal/estoque/movimentos`.
- `GET /api/v4/internal/estoque/{pecaUuid}`.
- `GET /api/v4/internal/estoque/abaixo-minimo`.
- Preparada para consumidores futuros de estoque.

### 2.5 Infraestrutura e entrega

- Migrations Flyway finais.
- Seed de estoque inicial.
- Docker Compose local com PostgreSQL.
- README, `.gitignore` e documentação em `docs/`.
- CI com `mvn verify`.
- Migração de testes e Postman na branch final.

---

## 3. Fora de escopo

| Item | Onde pertence |
|---|---|
| Cadastro de clientes | `Mekano-Registry-Service-API` |
| Cadastro de veículos | `Mekano-Registry-Service-API` |
| Cadastro de peças (catálogo) | `Mekano-Registry-Service-API` |
| Cadastro de serviços | `Mekano-Registry-Service-API` |
| Ordens de serviço | Outro repositório (fase futura) |
| Orçamentos | Outro repositório (fase futura) |
| Pagamentos | Outro repositório (fase futura) |
| WhatsApp | Outro repositório (fase futura) |
| Autenticação/autorização | Fora do escopo; apenas roles comentadas com `TODO` |

> O Supplies **não acessa** o banco do Registry. A única integração é o endpoint de batch de peças.

---

## 4. Mudança de escopo: estoque saiu do Registry e veio para o Supplies

Esta é uma decisão central da Fase 4 e deve ser compreendida por qualquer pessoa que evoluir o serviço.

### 4.1 Como era no monólito

No monólito Mekano, a entidade `Peca` acumulava:

- **catálogo** (código, descrição, valor);
- **estoque** (`saldoAtual`, `saldoReservado`, `estoqueMinimo`);
- **operações** de crédito, débito, reserva, liberação e consumo.

A NF de entrada chamava `creditarSaldo` na própria `Peca`, dentro do mesmo banco.

### 4.2 Como ficou na Fase 4

- **Registry:** apenas catálogo (`codigo`, `descricao`, `valorUnitario`).
- **Supplies:** estoque (`saldo_atual`, `saldo_reservado`, `estoque_minimo`) e movimentos.
- A peça é referenciada no Supplies por `peca_uuid` (sem FK cross-database).
- O catálogo é validado no Registry via `POST /api/v4/internal/pecas/batch`.

### 4.3 Por que essa mudança foi feita

1. **NF + estoque em transação local ACID**
   - Salvar a NF, creditar estoque e gravar movimentos ocorrem no mesmo banco.
   - Se algo falhar, o rollback é total.
   - Não é necessário flag `estoque_creditado` nem endpoint de reprocessamento.

2. **Cancelamento de NF com estorno local**
   - O estorno do estoque acontece na mesma transação do cancelamento.
   - Se não houver saldo suficiente, a operação falha com `409` e a NF permanece `ATIVA`.

3. **Reposição e requisição de compra no mesmo serviço**
   - Estoque mínimo, movimentos e requisição ficam coesos.
   - A criação de requisição por falta de estoque pode ser local no futuro.

4. **Registry permanece um CRUD de catálogo**
   - Sem acoplamento com o fluxo de compras.
   - Sem endpoint de estoque no Registry.

5. **Sem ciclo de dependência**
   - Supplies → Registry (batch).
   - Registry não chama Supplies.

### 4.4 Consequências e mitigações

| Consequência | Mitigação |
|---|---|
| Agregado “peça” dividido entre catálogo e estoque | `peca_uuid` como referência lógica; documentação explícita |
| Linha de estoque pode não existir para uma peça nova | `obterOuCriar` na primeira referência, com saldo 0 |
| Peça pode ser inativada no Registry | Batch retorna `isActive`; Supplies bloqueia novas operações e preserva histórico |
| Seed dividido entre os dois serviços | UUIDs fixos + seed de catálogo no Registry + seed de estoque no Supplies |
| Necessidade de validar peça no Registry | Batch interno com contrato versionado em `/api/v4` |

### 4.5 O que não mudou

- A peça continua tendo um UUID estável.
- O valor unitário continua no catálogo.
- A requisição continua com itens, quantidades e status.
- A NF continua vinculada a uma requisição e usa os itens da requisição.

Explicação completa em [`adr/ADR-003-ownership-estoque.md`](adr/ADR-003-ownership-estoque.md).

---

## 5. Critérios de aceite do serviço

- [ ] Estoque com saldo, reservado, mínimo e movimentos em `/api/v4`.
- [ ] Requisição de compra: criar, listar, buscar, enviar, receber e cancelar.
- [x] NF de entrada: registrar, listar, buscar e cancelar.
- [x] NF e estoque na mesma transação local.
- [x] Cancelamento de NF com estorno e `409` em saldo insuficiente.
- [x] Idempotência no ledger de estoque.
- [x] Integração com o batch do Registry.
- [x] Migrations finais aplicadas do zero.
- [x] Seed de estoque inicial.
- [ ] DBeaver conectando em `localhost:5434/mekano_supplies`.
- [ ] README e `docs/` completos.
- [ ] CI verde.
- [ ] Nenhum push direto em `main`.
- [ ] Testes e Postman migrados na branch final `test/sprint-5-testes-e-postman`.

---

## 6. Premissas

- Não há dados a migrar: o projeto é acadêmico e reinicia a cada fase.
- As migrations podem ser escritas já no estado final.
- A autenticação não faz parte desta fase.
- A comunicação com o Registry é síncrona, REST/JSON, em `/api/v4`.
- O estoque é local ao Supplies; não há transação distribuída.
