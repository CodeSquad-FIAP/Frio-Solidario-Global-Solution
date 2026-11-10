-- FRIO SOLIDARIO - GLOBAL SOLUTION FIAP
-- Consultas SQL com JOIN e agregacoes
-- Banco de dados: PostgreSQL 17
-- As consultas utilizam as tabelas do schema.sql
-- e os dados carregados pelo seed.sql.

-- CONSULTA 1 - QUILOS DOADOS POR INSTITUICAO


-- Objetivo:
-- Calcular quantos quilos de alimentos foram
-- distribuidos por cada instituicao nos ultimos
-- 30 dias.

-- Decisao apoiada:
-- Identificar as instituicoes com maior volume
-- de doacoes e acompanhar o impacto social.

SELECT
    i.id AS instituicao_id,
    i.nome AS instituicao,
    COUNT(l.id) AS lotes_doados,
    SUM(l.kg) AS kg_doados_30_dias

FROM instituicao i

INNER JOIN lote l
    ON l.instituicao_id = i.id

WHERE l.status = 'DISTRIBUIDO'
  AND l.retirado_em >=
      CURRENT_TIMESTAMP - INTERVAL '30 days'
  AND l.retirado_em <= CURRENT_TIMESTAMP

GROUP BY
    i.id,
    i.nome

ORDER BY
    kg_doados_30_dias DESC;


-- CONSULTA 2 - EQUIPAMENTOS COM MAIS ALERTAS


-- Objetivo:
-- Identificar equipamentos com maior numero
-- de alertas criticos nos ultimos 30 dias.

-- Decisao apoiada:
-- Priorizar manutencao preventiva e corretiva
-- nos equipamentos mais problematicos.

SELECT
    e.id AS equipamento_id,
    e.nome AS equipamento,
    COUNT(a.id) AS total_alertas_criticos

FROM equipamento e

INNER JOIN alerta a
    ON a.equipamento_id = e.id

WHERE a.severidade = 'CRITICA'
  AND a.criado_em >=
      CURRENT_TIMESTAMP - INTERVAL '30 days'
  AND a.criado_em <= CURRENT_TIMESTAMP

GROUP BY
    e.id,
    e.nome

ORDER BY
    total_alertas_criticos DESC,
    e.nome;


-- CONSULTA 3 - TEMPO MEDIO DE RESOLUCAO


-- Objetivo:
-- Calcular o tempo medio, em minutos,
-- necessario para resolver os alertas
-- de cada instituicao nos ultimos 30 dias.

-- Decisao apoiada:
-- Avaliar a eficiencia das equipes e identificar
-- onde o atendimento precisa melhorar.

SELECT
    i.id AS instituicao_id,
    i.nome AS instituicao,

    COUNT(a.id) AS alertas_resolvidos,

    ROUND(
        AVG(
            EXTRACT(
                EPOCH FROM
                (a.resolvido_em - a.criado_em)
            ) / 60.0
        ),
        2
    ) AS tempo_medio_minutos

FROM instituicao i

LEFT JOIN equipamento e
    ON e.instituicao_id = i.id

LEFT JOIN alerta a
    ON a.equipamento_id = e.id
    AND a.status = 'RESOLVIDO'
    AND a.criado_em >=
        CURRENT_TIMESTAMP - INTERVAL '30 days'
    AND a.criado_em <= CURRENT_TIMESTAMP

GROUP BY
    i.id,
    i.nome

ORDER BY
    tempo_medio_minutos DESC NULLS LAST,
    i.nome;


-- CONSULTA 4 - LEITURAS FORA DA FAIXA


-- Objetivo:
-- Calcular o percentual de leituras de
-- temperatura fora da faixa configurada
-- para cada equipamento nos ultimos 30 dias.

-- Decisao apoiada:
-- Identificar equipamentos com maior risco
-- de comprometer a conservacao dos alimentos.

SELECT
    e.id AS equipamento_id,
    e.nome AS equipamento,

    COUNT(l.id) AS total_leituras,

    SUM(
        CASE
            WHEN l.temperatura < e.temp_min
              OR l.temperatura > e.temp_max
            THEN 1
            ELSE 0
        END
    ) AS leituras_fora_faixa,

    ROUND(
        100.0 *
        SUM(
            CASE
                WHEN l.temperatura < e.temp_min
                  OR l.temperatura > e.temp_max
                THEN 1
                ELSE 0
            END
        ) / NULLIF(COUNT(l.id), 0),
        2
    ) AS percentual_fora_faixa

FROM equipamento e

INNER JOIN leitura l
    ON l.equipamento_id = e.id

WHERE l.registrada_em >=
      CURRENT_TIMESTAMP - INTERVAL '30 days'
  AND l.registrada_em <= CURRENT_TIMESTAMP

GROUP BY
    e.id,
    e.nome

ORDER BY
    percentual_fora_faixa DESC,
    e.nome;


-- CONSULTA 5 - TAXA DE PERDA POR CATEGORIA


-- Objetivo:
-- Calcular o percentual de lotes perdidos
-- em relacao ao total de lotes de cada
-- categoria de alimento.

-- Decisao apoiada:
-- Identificar categorias com perdas registradas
-- e priorizar melhorias de armazenamento.

SELECT
    l.categoria,

    COUNT(*) AS total_lotes,

    SUM(
        CASE
            WHEN l.status = 'PERDIDO'
            THEN 1
            ELSE 0
        END
    ) AS lotes_perdidos,

    ROUND(
        100.0 *
        SUM(
            CASE
                WHEN l.status = 'PERDIDO'
                THEN 1
                ELSE 0
            END
        ) / NULLIF(COUNT(*), 0),
        2
    ) AS taxa_perda_percentual,

    SUM(
        CASE
            WHEN l.status = 'PERDIDO'
            THEN l.kg
            ELSE 0
        END
    ) AS kg_perdidos

FROM lote l

GROUP BY
    l.categoria

HAVING
    SUM(
        CASE
            WHEN l.status = 'PERDIDO'
            THEN 1
            ELSE 0
        END
    ) > 0

ORDER BY
    taxa_perda_percentual DESC,
    kg_perdidos DESC;
