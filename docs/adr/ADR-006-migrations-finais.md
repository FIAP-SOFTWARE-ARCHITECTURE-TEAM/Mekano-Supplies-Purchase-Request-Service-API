# ADR-006 - Migrations finais e seed

- **Status:** Aceito
- **Data:** 2026-11-27
- **Contexto:** Sprint 0/Sprint 1

## Contexto

O projeto academico da Fase 4 foi reiniciado como um novo servico. Nao existem
dados de producao a migrar. O schema precisa ser reproduzivel a partir do zero
e as migrations devem refletir o modelo final, sem historico de correcoes.

## Decisao

- **Flyway** com migrations versionadas em
  `supplies-infrastructure/src/main/resources/db/migration`.
- **Uma migration final por tabela**, ja com colunas, constraints e indices.
- Sem migrations de correcao (`ALTER`) nesta fase academica.
- Migrations da Sprint 1:
  - `V1__create_requisicoes_compra.sql`
  - `V2__create_itens_requisicao_compra.sql`
- Migrations das proximas sprints seguem a numeracao `V3+`.
- `quarkus.flyway.migrate-at-start=true` em producao/desenvolvimento.
- Testes automatizados usam H2 com `drop-and-create` e Flyway desabilitado,
  para velocidade e independencia de Docker.
- Reset local: `docker compose down -v && docker compose up -d --build`.

## Consequencias

- O banco sobe do zero de forma deterministica.
- Alteracoes de schema durante a fase devem ser feitas na migration original,
  com recriacao do volume local.
- Nao ha risco de migrations divergentes entre ambientes academicos.
- A ausencia de migrations corretivas exige disciplina: cada alteracao precisa
  ser consolidada antes do merge da sprint.

## Alternativas consideradas

- **Hibernate `update`:** descartado por nao versionar schema e mascarar
  divergencias.
- **Migrations incrementais de correcao:** descartadas por nao haver dados a
  preservar e por dificultar a leitura do modelo final.
