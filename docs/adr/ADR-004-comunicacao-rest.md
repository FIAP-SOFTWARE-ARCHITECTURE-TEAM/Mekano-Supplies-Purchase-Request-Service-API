# ADR-004 - Comunicacao REST/JSON sincrona com o Registry

- **Status:** Aceito
- **Data:** 2026-11-27
- **Contexto:** Sprint 1 - validacao de pecas da requisicao de compra

## Contexto

Ao criar uma requisicao de compra, o Supplies precisa validar se as pecas
existem e estao ativas, alem de capturar snapshots de codigo, descricao e valor
unitario. O catalogo e propriedade do `Mekano-Registry-Service-API`.

## Decisao

- Comunicacao **REST/JSON sincrona** via MicroProfile REST Client.
- Endpoint do Registry: `POST /api/v4/internal/pecas/batch`.
- O Supplies envia a lista de `pecaUuids` em uma unica chamada (batch),
  evitando N+1.
- Configuracao:
  - `registry.base-url` (default `http://localhost:8081`);
  - `registry.timeout-ms` (default `3000`);
  - `quarkus.rest-client.registry-api.*`.
- Mapeamento de erros:
  - peca inexistente -> `404`;
  - peca inativa -> `409`;
  - Registry indisponivel, timeout ou resposta invalida -> `503`.
- Contrato coberto por testes com **WireMock**.
- Para desenvolvimento sem o Registry implementado existe o
  `StubPecaCatalogAdapter`, habilitado por `supplies.registry.stub=true`.

## Consequencias

- Simplicidade operacional: sem broker ou event sourcing nesta fase.
- A criacao da requisicao depende da disponibilidade do Registry; falhas sao
  traduzidas para `503` e o cliente pode tentar novamente.
- O batch reduz latencia e carga no Registry.
- Snapshots sao gravados no item, portanto `GET` de requisicao nao consulta o
  Registry.
- O acoplamento e unidirecional: Supplies -> Registry. O Registry nao chama o
  Supplies.

## Alternativas consideradas

- **Mensageria assincrona:** descartada por adicionar complexidade sem ganho
  nesta fase.
- **Consulta peca a peca:** descartada por latencia e carga desnecessaria.
- **Copia do catalogo para o Supplies:** descartada por duplicar a fonte de
  verdade.
