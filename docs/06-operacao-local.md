# Operação local — Mekano Supplies Purchase Request Service API

**API:** `http://localhost:8082/api/v4`
**Banco:** `mekano_supplies` em `localhost:5434`
**Registry:** `http://localhost:8081/api/v4`

---

## 1. Pré-requisitos

- Java 17
- Docker Desktop
- Maven Wrapper (`mvnw` / `mvnw.cmd`)
- DBeaver (opcional)
- Postman/Newman (opcional)
- Registry rodando (para validação de peças)

---

## 2. Subir o Registry primeiro

```bash
cd ../Mekano-Registry-Service-API
docker compose up -d --build
```

Confirme:

```bash
curl http://localhost:8081/q/health
```

---

## 3. Subir o Supplies

```bash
docker compose up -d --build
```

Serviços:

| Serviço | Container | Porta |
|---|---|---|
| PostgreSQL | `supplies-db` | `5434` |
| API | `supplies-api` | `8082` |

Verificar:

```bash
docker compose ps
docker compose logs -f supplies-api
```

---

## 4. Rodar apenas o banco e a API em modo dev

```bash
docker compose up -d supplies-db
./mvnw quarkus:dev -pl supplies-rest -am
```

Windows:

```powershell
docker compose up -d supplies-db
.\mvnw.cmd quarkus:dev -pl supplies-rest -am
```

---

## 5. Variáveis de ambiente

Crie `.env` a partir de `.env.example`:

```properties
SUPPLIES_DB_NAME=mekano_supplies
SUPPLIES_DB_USER=mekano
SUPPLIES_DB_PASSWORD=mekano
SUPPLIES_DB_PORT=5434
SUPPLIES_API_PORT=8082
REGISTRY_URL=http://localhost:8081
```

No `application.properties`:

```properties
quarkus.rest.path=/api/v4
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5434/mekano_supplies
quarkus.datasource.username=mekano
quarkus.datasource.password=mekano
quarkus.flyway.migrate-at-start=true

registry.base-url=${REGISTRY_URL:http://localhost:8081}
registry.timeout-ms=3000
```

> As credenciais acima são **somente para desenvolvimento local**. Não usar em produção.

---

## 6. DBeaver

Após subir o container:

1. **Database → New Database Connection → PostgreSQL**.
2. Preencha:

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Port | `5434` |
| Database | `mekano_supplies` |
| Username | `mekano` |
| Password | `mekano` |

3. Clique em **Test Connection**.
4. Finalize e expanda `public` para ver as tabelas.

Tabelas esperadas após o seed:

- `requisicoes_compra`
- `itens_requisicao_compra`
- `estoque`
- `movimentos_estoque`
- `nf_entradas`
- `flyway_schema_history`

---

## 7. Endpoints úteis

| Recurso | URL |
|---|---|
| API | `http://localhost:8082/api/v4` |
| Swagger UI | `http://localhost:8082/q/swagger-ui` |
| OpenAPI | `http://localhost:8082/q/openapi` |
| Health | `http://localhost:8082/q/health` |
| Metrics | `http://localhost:8082/q/metrics` |

---

## 8. Seed e reset

O seed é aplicado pela migration `V6__seed_estoque_inicial.sql`.

Para resetar:

```bash
docker compose down -v
docker compose up -d --build
```

> `down -v` apaga o volume do PostgreSQL. Use apenas em desenvolvimento.

---

## 9. Postman

1. Suba o Registry e o Supplies.
2. Importe `docs/postman/Mekano-Supplies.postman_collection.json`.
3. Importe o environment `Mekano Supplies Local`.
4. Confirme:
   - `baseUrl = http://localhost:8082/api/v4`
   - `registryUrl = http://localhost:8081/api/v4`
5. Execute o fluxo:
   - criar requisição;
   - enviar;
   - receber;
   - registrar NF;
   - consultar estoque;
   - cancelar NF;
   - consultar estoque novamente.

---

## 10. Troubleshooting

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Connection refused` na API | Container não subiu | `docker compose logs supplies-api` |
| `Flyway validation failed` | Migration alterada após aplicação | `docker compose down -v` e subir de novo |
| `Port 5434 already in use` | Outro PostgreSQL local | Alterar `SUPPLIES_DB_PORT` ou parar o outro serviço |
| `Port 8082 already in use` | Outra API rodando | Alterar `SUPPLIES_API_PORT` |
| Criação de requisição retorna `503` | Registry indisponível | Subir o Registry e conferir `REGISTRY_URL` |
| DBeaver não conecta | Container parado ou senha errada | `docker compose ps` e conferir credenciais |
| Seed não aparece | Volume antigo | `docker compose down -v` e subir de novo |
| NF não credita estoque | Requisição não está `PRODUTO_RECEBIDO` | Enviar e receber a requisição antes da NF |

---

## 11. Limpeza

```bash
docker compose down
docker compose down -v   # remove volumes
docker system prune -f   # opcional
```
