# ADR-002 - PostgreSQL 16

- **Status:** Aceito
- **Data:** 2026-11-27
- **Contexto:** Sprint 0/Sprint 1

## Contexto

O Supplies precisa garantir consistencia transacional entre requisicao de
compra, estoque e NF de entrada. O fluxo de NF exige que salvar a NF, creditar
estoque e gravar movimentos ocorram na mesma transacao local. Tambem ha
necessidade de constraints, indices e concorrencia controlada.

## Decisao

- **PostgreSQL 16** como banco de dados oficial.
- Banco `mekano_supplies`.
- Porta local `5434` (mapeada para `5432` no container).
- Driver `quarkus-jdbc-postgresql`.
- Hibernate ORM com Panache para persistencia.
- **Sem FK cross-database** para `peca_uuid`: a peca pertence ao Registry e e
  referenciada apenas logicamente.
- Testes automatizados usam **H2 em modo PostgreSQL** (`MODE=PostgreSQL`) para
  nao depender de Docker no CI/local. A validacao com PostgreSQL real e feita
  via `docker compose` e Flyway.

## Consequencias

- Transacoes ACID locais para NF + estoque, sem transacao distribuida.
- Constraints e indices sao definidos explicitamente nas migrations.
- O ambiente de teste e rapido e deterministico, mas o dialeto H2 nao substitui
  a validacao final no PostgreSQL. O CI executa os testes com H2 e o compose
  local sobe PostgreSQL 16 para validacao manual.
- `peca_uuid` pode ficar orfao no Supplies caso a peca seja removida do
  Registry; isso e aceitavel porque os snapshots preservam codigo, descricao e
  valor no item da requisicao.

## Alternativas consideradas

- **MySQL/MariaDB:** descartado por menor aderencia a recursos usados no
  projeto (constraints, `ON CONFLICT`, tipos).
- **Banco embarcado em producao:** descartado por falta de robustez para
  concorrencia e operacao.
