# Banco do zero em máquina limpa — `schema.sql` + `seed.sql`

**O que prova:** o critério do card "Script `.sql` com constraints e carga inicial" — *roda do zero sem
erro em máquina limpa*. Container novo, banco vazio, só os dois scripts.

**Ambiente:** Docker `postgres:17` (PostgreSQL **17.11**, Debian 13), scripts montados em `/sql` somente
leitura, 12/11/2026 00:06 (America/Sao_Paulo), commit `52f2e49`.

```console
$ docker run -d --name frio-pg-log -e POSTGRES_PASSWORD=*** -e POSTGRES_DB=frio_solidario \
    -v "$(pwd -W)/db:/sql:ro" postgres:17
$ docker exec frio-pg-log psql -U postgres -t -A -c "select version();"
PostgreSQL 17.11 (Debian 17.11-1.pgdg13+2) on x86_64-pc-linux-gnu, compiled by gcc (Debian 14.2.0-19) 14.2.0, 64-bit

$ docker exec frio-pg-log psql -U postgres -d frio_solidario -X -v ON_ERROR_STOP=1 -f /sql/schema.sql
BEGIN
CREATE TABLE          <- 6 tabelas
CREATE TABLE
CREATE TABLE
CREATE TABLE
CREATE TABLE
CREATE TABLE
CREATE INDEX          <- 5 índices
CREATE INDEX
CREATE INDEX
CREATE INDEX
CREATE INDEX
COMMIT
```

```console
$ docker exec frio-pg-log psql -U postgres -d frio_solidario -X -v ON_ERROR_STOP=1 -f /sql/seed.sql
BEGIN
INSERT 0 3            <- instituicao
INSERT 0 6            <- usuario
INSERT 0 6            <- equipamento
INSERT 0 30           <- leitura
INSERT 0 5            <- alerta
INSERT 0 12           <- lote
 setval
--------
      3
(1 row)
...
 setval
--------
     12
(1 row)

COMMIT
```

**Saída dos dois scripts:** `exit 0`, nenhum `ERROR` (13 linhas no schema, 38 no seed). Depois da carga:

| Objeto | Quantidade |
|---|---|
| Tabelas | 6 |
| Chaves estrangeiras | 7 |
| `CHECK`s | 58 |
| Índices | 14 |

| Tabela | Linhas |
|---|---|
| instituicao | 3 |
| usuario | 6 |
| equipamento | 6 |
| leitura | 30 |
| alerta | 5 |
| lote | 12 |

**As constraints recusam dado inválido** (o script não é só `CREATE TABLE`):

```console
$ docker exec frio-pg-log psql -U postgres -d frio_solidario -X \
    -c "insert into leitura (equipamento_id, temperatura, umidade, porta_aberta, registrada_em, origem) \
        values (1, 100, 50, false, now(), 'SENSOR');"
ERROR:  new row for relation "leitura" violates check constraint "ck_leitura_temperatura"
DETAIL:  Failing row contains (31, 1, 100.00, 50.00, f, 2026-11-12 00:06:21.105089+00, SENSOR).
```

**Limite conhecido:** os scripts assumem **banco vazio**. Rodar o `schema.sql` de novo no mesmo banco
falha com `relation "instituicao" already exists` — não há `DROP ... CASCADE` no topo, de propósito, para
não apagar histórico por acidente. Para recomeçar: `docker rm -f frio-pg` e subir o container outra vez.
