# Plano de tarefas — Mekano Supplies Purchase Request Service API

**Fase 4** · **API prefix:** `/api/v4` · **Banco:** `mekano_supplies` · **Porta:** `8082`

---

## 1. Estratégia de branches

Cada sprint é entregue em **uma branch por repositório**, com **PR obrigatório** para `main`. Nunca fazer push direto em `main`.

| Branch | Sprint | Objetivo |
|---|---|---|
| `start` | Sprint 0 | Estrutura inicial, docs, README, `.gitignore`, POM, compose e CI |
| `feature/sprint-1-requisicoes-compra` | Sprint 1 | Requisição de compra: criar, listar e buscar |
| `feature/sprint-2-estoque-ciclo-requisicao` | Sprint 2 | Estoque, movimentos e ciclo da requisição |
| `feature/sprint-3-nf-entrada` | Sprint 3 | NF de entrada e cancelamento com estorno |
| `feature/sprint-4-integracao-entrega` | Sprint 4 | Integração, hardening e release |
| `test/sprint-5-testes-e-postman` | Sprint 5 | Migração/organização final dos testes e Postman |

Regras:

- Exceção única de commit direto em `main`: commit inicial vazio para viabilizar o primeiro PR.
- Merge somente com CI verde.
- A branch `start` é a base do Sprint 0 e já contém README, `.gitignore` e `docs/`.
- A branch `test/sprint-5-testes-e-postman` é criada **depois** do Sprint 4, quando toda a implementação estiver concluída.
- O Sprint 2 do Supplies depende do Sprint 2 do Registry (batch de peças).

---

## 2. Sprint 0 — Fundação (`start`)

### Objetivo

Criar a base do projeto, a documentação e o ambiente local.

### Tarefas

- [x] Criar commit inicial vazio em `main`.
- [x] Criar branch `start`.
- [x] Criar `README.md`.
- [x] Criar `.gitignore`.
- [x] Criar `docs/` com escopo, arquitetura, plano, modelo, contratos, testes, operação, decisões e ADRs.
- [x] Criar POM pai e módulos:
  - `supplies-domain`
  - `supplies-application`
  - `supplies-infrastructure`
  - `supplies-rest`
- [x] Configurar Java 17, Quarkus 3.36.0, Lombok, MapStruct, Jandex, Surefire e JaCoCo.
- [x] Configurar `quarkus.rest.path=/api/v4`.
- [x] Configurar datasource PostgreSQL, Flyway, OpenAPI, health, metrics e logs.
- [x] Configurar `registry.base-url` e timeout do cliente.
- [x] Criar `docker-compose.yml` com `supplies-db` e `supplies-api`.
- [x] Criar `.env.example`.
- [x] Criar `Dockerfile`.
- [x] Criar CI `.github/workflows/ci.yml` com `mvn -B verify`.
- [x] Criar ADRs:
  - `ADR-001-stack.md`
  - `ADR-002-postgresql.md`
  - `ADR-003-ownership-estoque.md`
  - `ADR-004-comunicacao-rest.md`
  - `ADR-005-idempotencia-estoque.md`
  - `ADR-006-migrations-finais.md`
- [x] Criar `PecaCatalogPort` + stub/WireMock.
- [x] Abrir PR `start → main`.

### Critérios de aceite

- [x] `mvn -B verify` executa sem erros.
- [x] `docker compose config` válido.
- [ ] API sobe em `http://localhost:8082/api/v4` (não validado em runtime: daemon Docker indisponível).
- [x] `/q/health` responde `UP`.
- [x] README e `docs/` revisados.
- [x] Nenhum push direto em `main`.

---

## 3. Sprint 1 — Requisição de compra (`feature/sprint-1-requisicoes-compra`)

### Objetivo

Implementar o núcleo da requisição de compra, com validação de peças via Registry.

### Tarefas

#### Domínio

- [x] `RequisicaoCompra` com factories `criar`/`reconstitute`.
- [x] `ItemRequisicaoCompra`.
- [x] `StatusRequisicao`: `ABERTA`, `ENVIADA`, `COMPRA_APROVADA`, `PRODUTO_RECEBIDO`, `CANCELADA`.
- [x] `MotivoRequisicao`: `ESTOQUE_MINIMO`, `ORDEM_SERVICO`.
- [x] Regras:
  - pelo menos 1 item;
  - quantidade > 0;
  - sem peça duplicada;
  - criação pública sempre `ABERTA` e `motivo=ESTOQUE_MINIMO`.

#### Application

- [x] `RequisicaoCompraService`: criar, listar, buscar.
- [x] `PecaCatalogPort` para consulta em lote.
- [x] Validação de peças existentes/ativas.
- [x] Snapshot de `pecaCodigo`, `pecaDescricao` e `valorUnitario`.
- [x] `@Transactional` nos métodos de escrita.

#### Infrastructure

- [x] `RequisicaoCompraEntity`, `ItemRequisicaoCompraEntity`.
- [x] Repositórios Panache.
- [x] Mappers MapStruct.
- [x] Migrations:
  - `V1__create_requisicoes_compra.sql`
  - `V2__create_itens_requisicao_compra.sql`
- [x] `RegistryPecaClient` (MicroProfile REST Client) com:
  - `POST /api/v4/internal/pecas/batch`;
  - timeout;
  - tratamento de `404`/`5xx`;
  - WireMock nos testes.

#### REST

- [x] `RequisicaoCompraResource`:
  - `POST /api/v4/requisicoes-compra`;
  - `GET /api/v4/requisicoes-compra`;
  - `GET /api/v4/requisicoes-compra/{uuid}`.
- [x] DTOs de entrada/saída.
- [x] Paginação e filtros.
- [x] `@RolesAllowed({"admin", "financeiro"})` comentado.

#### Testes mínimos e documentação

- [x] Testes de domínio.
- [x] Testes de service com mock do `PecaCatalogPort`.
- [x] Testes de repositório.
- [x] Testes REST.
- [x] Teste de contrato do client com WireMock.
- [x] Atualizar `docs/03-modelo-de-dados.md` e `docs/04-contratos-api.md`.
- [ ] Abrir PR (push da branch realizado; PR pendente de criação no GitHub).

### Critérios de aceite

- [x] Criar/listar/buscar requisição em `/api/v4`.
- [x] Peça inexistente → `404`; inativa → `409`.
- [x] Snapshots gravados corretamente.
- [x] Migrations aplicadas do zero (validado em H2 modo PostgreSQL via `MigrationSmokeTest`; PostgreSQL real pendente de Docker).
- [ ] CI verde (workflow criado; aguardando execução no PR).

---

## 4. Sprint 2 — Estoque e ciclo da requisição (`feature/sprint-2-estoque-ciclo-requisicao`)

### Objetivo

Implementar o estoque local, os movimentos idempotentes e o ciclo de vida da requisição.

### Tarefas

#### Estoque

- [x] Domínio `Estoque`, `MovimentoEstoque`, `TipoMovimentoEstoque`, `OrigemMovimentoEstoque`.
- [x] Regras:
  - `CREDITO`: `saldo_atual += qtd`;
  - `DEBITO`: exige `saldo_atual >= qtd`;
  - `RESERVA`: `saldo_reservado += qtd`;
  - `LIBERACAO`: exige `saldo_reservado >= qtd`;
  - `CONSUMO`: exige ambos >= qtd.
- [x] `EstoqueService`:
  - `obterOuCriar`;
  - `movimentar`;
  - `listarAbaixoMinimo`.
- [x] `idempotency_key` única; duplicata retorna o movimento existente.
- [x] Controle de concorrência via lock pessimista na linha de estoque.
- [x] Entities/repos/mappers:
  - `EstoqueEntity`, `MovimentoEstoqueEntity`;
  - migrations `V3__create_estoque.sql` e `V4__create_movimentos_estoque.sql`.
- [x] API interna:
  - `POST /api/v4/internal/estoque/movimentos`;
  - `GET /api/v4/internal/estoque/{pecaUuid}`;
  - `GET /api/v4/internal/estoque/abaixo-minimo`.
- [x] `@RolesAllowed("service:estoque")` comentado.

#### Ciclo da requisição

- [x] `enviar`: `ABERTA → ENVIADA`.
- [x] `receber`: `ENVIADA`/`COMPRA_APROVADA → PRODUTO_RECEBIDO`.
- [x] `cancelar`: `ABERTA → CANCELADA`; bloquear `ORDEM_SERVICO`.
- [x] `409` para transições inválidas.
- [x] `obterOuCriar` estoque na criação da requisição.

#### Integração real

- [x] Fluxo de criação usa o client REST do Registry.
- [x] Contract test do batch.
- [x] Testes de concorrência e idempotência.
- [x] Atualizar `docs/03-modelo-de-dados.md`, `docs/04-contratos-api.md` e `docs/01-arquitetura.md`.
- [ ] Abrir PR.

### Critérios de aceite

- [x] Estoque e movimentos funcionando em `/api/v4/internal/estoque`.
- [x] Idempotência comprovada com teste.
- [x] Sem lost update em débitos concorrentes.
- [x] Ciclo da requisição completo.
- [x] Integração real com o batch do Registry.
- [ ] CI verde.

---

## 5. Sprint 3 — NF de entrada (`feature/sprint-3-nf-entrada`)

### Objetivo

Implementar a NF de entrada com crédito de estoque local e cancelamento com estorno.

### Tarefas

#### Domínio

- [x] `NfEntrada` com:
  - `chaveAcesso` (44 dígitos);
  - `valorTotal` (> 0);
  - `requisicaoUuid`;
  - `status` (`ATIVA`/`CANCELADA`);
  - `motivoCancelamento`, `dataCancelamento`.
- [x] Regras:
  - requisição precisa estar `PRODUTO_RECEBIDO`;
  - chave única;
  - valor > 0.

#### Application

- [x] `NfEntradaService`:
  - `registrar`;
  - `listar`;
  - `buscar`;
  - `cancelar`.
- [x] Registrar em transação local:
  - salvar NF `ATIVA`;
  - creditar estoque de cada item da requisição;
  - gravar movimentos com chave `nf:<uuid>:<pecaUuid>:CREDITO`.
- [x] Cancelar em transação local:
  - somente `ATIVA`;
  - debitar estoque de cada item;
  - gravar movimentos com chave `nf:<uuid>:<pecaUuid>:ESTORNO`;
  - marcar `CANCELADA`;
  - `409` se saldo insuficiente, com rollback.
- [x] **Sem** flag `estoque_creditado` e **sem** endpoint de reprocessamento.

#### Infrastructure

- [x] `NfEntradaEntity`, repositório, mapper.
- [x] Migration `V5__create_nf_entradas.sql`.
- [x] Seed `V6__seed_estoque_inicial.sql` com as duas peças.
- [x] Índices:
  - `chave_acesso` única;
  - `requisicao_uuid`;
  - `status`.

#### REST

- [x] `NfEntradaResource`:
  - `POST /api/v4/nf-entrada`;
  - `GET /api/v4/nf-entrada`;
  - `GET /api/v4/nf-entrada/{uuid}`;
  - `POST /api/v4/nf-entrada/{uuid}/cancelar`.
- [x] DTOs e validações.
- [x] `@RolesAllowed("admin")` comentado.

#### Testes mínimos e documentação

- [x] Testes de domínio.
- [x] Testes de service:
  - sucesso;
  - chave duplicada;
  - requisição em status inválido;
  - chave inválida;
  - valor <= 0;
  - cancelamento;
  - saldo insuficiente;
  - rollback.
- [x] Testes de repositório.
- [x] Testes REST.
- [x] Atualizar `docs/03-modelo-de-dados.md`, `docs/04-contratos-api.md` e `docs/07-decisoes-e-riscos.md`.
- [ ] Abrir PR.

### Critérios de aceite

- [x] NF registrada e estoque creditado na mesma transação.
- [x] NF cancelada e estoque estornado na mesma transação.
- [x] `409` em saldo insuficiente sem alterar NF nem estoque.
- [x] Seed de estoque aplicado.
- [x] CI verde (local: `./mvnw clean test`).

---

## 6. Sprint 4 — Integração e entrega (`feature/sprint-4-integracao-entrega`)

### Objetivo

Validar a integração com o Registry, fazer hardening e preparar a release.

### Tarefas

- [ ] Contract tests com o batch do Registry.
- [ ] E2E:
  1. Subir Registry e Supplies.
  2. Confirmar seed de catálogo e estoque.
  3. Criar requisição de `PEA-001` qtd 5.
  4. Enviar e receber.
  5. Registrar NF com chave de 44 dígitos.
  6. Confirmar estoque creditado (10 → 15).
  7. Cancelar NF e confirmar estorno (15 → 10).
  8. Validar `409` para chave duplicada, NF já cancelada e saldo insuficiente.
- [ ] Hardening de erros, timeout e logs.
- [ ] Auditoria de roles comentadas em todos os endpoints.
- [ ] README e docs finais.
- [ ] Diagramas Mermaid (arquitetura, fluxo de NF e estoque).
- [ ] Tag `v4.0.0` (release acadêmica da fase 4).
- [ ] PR final.

### Critérios de aceite

- [ ] `mvn -B verify` verde.
- [ ] Compose sobe os dois serviços.
- [ ] E2E completo verde.
- [ ] Documentação completa.
- [ ] Nenhum endpoint sem `@RolesAllowed` comentado.
- [ ] Release tagueada.

---

## 7. Sprint 5 — Testes e Postman (`test/sprint-5-testes-e-postman`)

> **Esta branch é criada após toda a implementação (Sprints 1–4).**
> O objetivo é migrar, organizar e estabilizar os testes automatizados e os JSONs do Postman.

### Objetivo

Concentrar a migração dos testes do monólito e a organização final dos artefatos de teste, sem adicionar novas funcionalidades.

### Tarefas

- [ ] Migrar testes de domínio de `../Mekano-API/mekano-domain/src/test`:
  - `RequisicaoCompraTest`;
  - `NfEntradaTest`;
  - partes de `PecaTest` relacionadas a estoque → novos `EstoqueTest`/`MovimentoEstoqueTest`.
- [ ] Migrar testes de application de `../Mekano-API/mekano-application/src/test`:
  - `RequisicaoCompraServiceTest`;
  - `NfEntradaServiceTest`;
  - partes de `PecaServiceTest` relacionadas a estoque → `EstoqueServiceTest`.
- [ ] Migrar testes de infrastructure de `../Mekano-API/mekano-infrastructure/src/test`:
  - `RequisicaoCompraRepositoryImplTest`;
  - `NfEntradaRepositoryImplTest`;
  - partes de `PecaRepositoryImplTest` relacionadas a estoque → `EstoqueRepositoryImplTest`/`MovimentoEstoqueRepositoryImplTest`;
  - `EstoqueMinimoObserverTest` → adaptar para o fluxo local de estoque mínimo (se incluído).
- [ ] Migrar testes REST de `../Mekano-API/mekano-rest/src/test`:
  - `RequisicaoCompraResourceTest`;
  - `NfEntradaResourceTest`;
  - partes de `PecaResourceTest` relacionadas a estoque → `EstoqueResourceTest`;
  - `AlertaResourceTest` → endpoint de abaixo do mínimo no Supplies.
- [ ] **Não migrar** testes de OS, orçamento, pagamento, WhatsApp e autenticação.
- [ ] Adaptar pacotes para `com.fiap.mekano.supplies.*`.
- [ ] Adaptar paths para `/api/v4`.
- [ ] Criar testes novos:
  - concorrência de estoque;
  - idempotência;
  - NF + estoque em transação local;
  - cancelamento com rollback.
- [ ] Migrar o JSON do Postman de `../Mekano-API/newman/Mekano_API_V2.0.postman_collection.json` para `docs/postman/Mekano-Supplies.postman_collection.json`.
- [ ] Separar apenas as requisições do Supplies; remover as de outros domínios.
- [ ] Atualizar base URL para `http://localhost:8082/api/v4`.
- [ ] Atualizar variáveis e UUIDs do seed.
- [ ] Criar environment com `baseUrl` e `registryUrl`.
- [ ] Organizar os testes por módulo e por tipo.
- [ ] Atualizar CI para rodar a suíte completa.
- [ ] Configurar cobertura JaCoCo.
- [ ] Atualizar `docs/05-testes-e-postman.md`.
- [ ] Abrir PR.

### Critérios de aceite

- [ ] Suíte completa verde no CI.
- [ ] Nenhum teste de domínio fora do escopo do Supplies.
- [ ] Postman do Supplies executável com os dois serviços e o seed.
- [ ] Documentação de testes e Postman atualizada.
- [ ] Branch `test/sprint-5-testes-e-postman` mergeada após o Sprint 4.

---

## 8. Ordem de merge

```text
Sprint 0 (start)
   ↓
Sprint 1 (requisições)
   ↓
Sprint 2 (estoque/ciclo)  ← depende do Sprint 2 do Registry (batch)
   ↓
Sprint 3 (NF)
   ↓
Sprint 4 (integração/release)
   ↓
Sprint 5 (testes/Postman) — branch final
```

O Sprint 2 do Registry deve ser mergeado **antes** do Sprint 2 do Supplies, pois o Supplies consome o batch de peças.

---

## 9. Definition of Done (por sprint)

- [ ] Código compilando e testes verdes.
- [ ] Migrations aplicadas do zero.
- [ ] Documentação atualizada em `docs/`.
- [ ] README atualizado quando necessário.
- [ ] `@RolesAllowed` comentado com `TODO` nos novos endpoints.
- [ ] Sem push direto em `main`.
- [ ] PR revisado e mergeado.
- [ ] Nenhum acesso ao banco do Registry.
- [ ] Prefixo `/api/v4` respeitado.
- [ ] NF e estoque sempre na mesma transação local.
