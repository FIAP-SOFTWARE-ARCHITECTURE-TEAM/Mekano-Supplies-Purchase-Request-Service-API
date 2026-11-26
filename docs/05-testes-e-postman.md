# Testes e Postman — Mekano Supplies Purchase Request Service API

**Branch final:** `test/sprint-5-testes-e-postman`
**Base URL:** `http://localhost:8082/api/v4`
**Registry:** `http://localhost:8081/api/v4`

---

## 1. Estratégia

A implementação (Sprints 1–4) mantém o build verde com testes mínimos das novas classes. A **migração completa** dos testes existentes do monólito e a **organização dos JSONs do Postman** são concentradas na branch final `test/sprint-5-testes-e-postman`, após o Sprint 4.

Tipos de teste:

| Tipo | Ferramenta | O que cobre |
|---|---|---|
| Unitário de domínio | JUnit 5 + AssertJ | Requisição, NF, estoque, movimentos |
| Unitário de application | JUnit 5 + Mockito | Services, validações, ports |
| Integração | QuarkusTest + PostgreSQL (Dev Services/Testcontainers) | Repositórios, migrations, mappers |
| REST | QuarkusTest + RestAssured | Resources, DTOs, status, Problem Details |
| Contrato | WireMock | Cliente do batch do Registry |
| Concorrência | JUnit 5 + transações paralelas | Débitos/reservas sem lost update |
| Idempotência | JUnit 5 + banco real | Mesma `idempotency_key` não duplica |
| E2E | Postman/Newman | Registry + Supplies |

---

## 2. Migração dos testes do monólito

Origem: `../Mekano-API/*/src/test`.

### 2.1 Domínio (`mekano-domain/src/test`)

| Teste de origem | Destino no Supplies | Observação |
|---|---|---|
| `RequisicaoCompraTest` | `supplies-domain` | Migrar integralmente |
| `NfEntradaTest` | `supplies-domain` | Adaptar para incluir status/cancelamento |
| `PecaTest` | `supplies-domain` | Migrar **somente as partes de estoque** para `EstoqueTest`/`MovimentoEstoqueTest` |
| `DomainRecordsTest` | `supplies-domain` | Adaptar aos records do Supplies |

**Não migrar:** `ClienteTest`, `VeiculoTest`, `ServicoTest`, `UserTest`, `OrcamentoTest`, `ItemOrcamentoTest`, `OrdemDeServico*`, eventos de OS/pagamento.

### 2.2 Application (`mekano-application/src/test`)

| Teste de origem | Destino no Supplies | Observação |
|---|---|---|
| `RequisicaoCompraServiceTest` | `supplies-application` | Migrar integralmente |
| `NfEntradaServiceTest` | `supplies-application` | Adaptar para estoque local e cancelamento |
| `PecaServiceTest` | `supplies-application` | Migrar **somente as partes de estoque** para `EstoqueServiceTest` |

**Não migrar:** `ClienteServiceTest`, `VeiculoServiceTest`, `ServicoServiceTest`, `OrcamentoServiceTest`, `OrdemDeServicoServiceTest`, `MockPaymentServiceTest`, `WhatsApp*`, `Auth*`, `AdminUserServiceTest`, `OsAudit*`, `CobrancaEmitidaListenerTest`.

### 2.3 Infrastructure (`mekano-infrastructure/src/test`)

| Teste de origem | Destino no Supplies | Observação |
|---|---|---|
| `RequisicaoCompraRepositoryImplTest` | `supplies-infrastructure` | Migrar integralmente |
| `NfEntradaRepositoryImplTest` | `supplies-infrastructure` | Adaptar para status/cancelamento |
| `PecaRepositoryImplTest` | `supplies-infrastructure` | Migrar **somente as partes de estoque** para `EstoqueRepositoryImplTest`/`MovimentoEstoqueRepositoryImplTest` |
| `EstoqueMinimoObserverTest` | `supplies-infrastructure` | Adaptar para o fluxo local de estoque mínimo (se incluído) |
| `AuditoriaListenerTest` | `supplies-infrastructure` | Migrar integralmente |
| `AuditoriaListenerIntegrationTest` | `supplies-infrastructure` | Migrar integralmente |
| `AuditoriaOrigemTest` | `supplies-infrastructure` | Migrar integralmente |

**Não migrar:** `ClienteRepositoryImplTest`, `VeiculoRepositoryImplTest`, `ServicoRepositoryImplTest`, `OrcamentoRepositoryImplTest`, `OrdemDeServicoRepositoryImplTest`, `SlaExpiryJobTest`, `PecaOrcamentoObserverTest`, `WhatsApp*`, `Pagamento*`, `OS*`, `RefreshToken*`, `User*`.

### 2.4 REST (`mekano-rest/src/test`)

| Teste de origem | Destino no Supplies | Observação |
|---|---|---|
| `RequisicaoCompraResourceTest` | `supplies-rest` | Adaptar path para `/api/v4` |
| `NfEntradaResourceTest` | `supplies-rest` | Adaptar path e incluir cancelamento |
| `PecaResourceTest` | `supplies-rest` | Migrar **somente as partes de estoque** para `EstoqueResourceTest` |
| `AlertaResourceTest` | `supplies-rest` | Adaptar para `GET /api/v4/internal/estoque/abaixo-minimo` |
| `FaultToleranceTest` | `supplies-rest` | Migrar apenas cenários de requisição/NF/estoque |
| `AuditoriaAuditFieldsTest` | `supplies-rest` | Adaptar entidades do Supplies |

**Não migrar:** `ClienteResourceTest`, `VeiculoResourceTest`, `VeiculoGetResourceTest`, `ServicoResourceTest`, `Orcamento*`, `OrdemDeServico*`, `Pagamento*`, `AdminUser*`, `WebhookEvolution*`, `LiberadoParaExecucaoTest`, `OsAuditResourceTest`, `ObservabilityEndpointsTest` (recriar se necessário), `ClasspathDiagnosticTest`.

---

## 3. Testes novos obrigatórios

- [ ] Concorrência:
  - dois `DEBITO` simultâneos não podem deixar saldo negativo;
  - duas `RESERVA` simultâneas não podem perder atualização.
- [ ] Idempotência:
  - mesma `idempotency_key` duas vezes → um único movimento;
  - `idempotente=true` na segunda chamada.
- [ ] NF + estoque:
  - falha ao salvar NF → estoque não é creditado;
  - falha ao creditar estoque → NF não é salva;
  - cancelamento com saldo insuficiente → rollback e NF `ATIVA`.
- [ ] Contrato do Registry:
  - batch com peças encontradas/não encontradas;
  - peça inativa;
  - timeout/5xx → `503`.
- [ ] Seed:
  - estoque inicial das duas peças.

---

## 4. Migração do Postman

Origem: `../Mekano-API/newman/Mekano_API_V2.0.postman_collection.json`.

Destino: `docs/postman/Mekano-Supplies.postman_collection.json`.

Passos:

1. Copiar a coleção de origem.
2. Manter apenas as requisições de:
   - requisição de compra;
   - NF de entrada;
   - estoque interno;
   - consulta de peças via Registry (batch), se necessário para o fluxo.
3. Remover requisições de clientes, veículos, serviços, OS, orçamento, pagamento, WhatsApp e auth.
4. Alterar a `baseUrl` para `http://localhost:8082/api/v4`.
5. Criar variável `registryUrl = http://localhost:8081/api/v4`.
6. Atualizar variáveis:
   - `peca1Uuid = 55555555-5555-5555-5555-555555555555`
   - `peca2Uuid = 66666666-6666-6666-6666-666666666666`
7. Criar environment `Mekano Supplies Local`.
8. Fluxo E2E:
   - criar requisição de `PEA-001` qtd 5;
   - enviar;
   - receber;
   - registrar NF;
   - consultar estoque (10 → 15);
   - cancelar NF;
   - consultar estoque (15 → 10).
9. Validar a coleção com os dois serviços no ar.
10. Documentar no README e em `docs/06-operacao-local.md`.

---

## 5. CI e cobertura

- Workflow: `.github/workflows/ci.yml`.
- Comando: `mvn -B verify`.
- PostgreSQL real via Dev Services/Testcontainers.
- JaCoCo configurado no POM pai.
- Cobertura mínima sugerida:
  - domínio e application: 80% de linhas;
  - infrastructure/rest: conforme viabilidade dos testes de integração.
- Falha de teste quebra o build.

---

## 6. Critérios de aceite da branch final

- [ ] Todos os testes migrados e verdes.
- [ ] Nenhum teste de domínio fora do escopo do Supplies.
- [ ] Paths `/api/v4` em todos os testes REST.
- [ ] Postman do Supplies executável com os dois serviços e o seed.
- [ ] Testes de concorrência e idempotência verdes.
- [ ] CI rodando a suíte completa.
- [ ] Cobertura configurada.
- [ ] Documentação atualizada.
- [ ] PR `test/sprint-5-testes-e-postman → main` mergeado após o Sprint 4.
