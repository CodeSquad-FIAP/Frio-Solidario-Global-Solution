-- Para quem já tem o banco carregado e não quer recriar: adiciona a ação tomada na resolução do
-- alerta (card "Lista de alertas com resolução"). Banco novo já nasce com a coluna pelo schema.sql.
--
--   psql -U postgres -d frio_solidario -v ON_ERROR_STOP=1 -f db/migracoes/2026-11-15-alerta-acao-tomada.sql

ALTER TABLE alerta
    ADD COLUMN IF NOT EXISTS acao_tomada VARCHAR(16);

ALTER TABLE alerta
    DROP CONSTRAINT IF EXISTS ck_alerta_acao_tomada;

ALTER TABLE alerta
    ADD CONSTRAINT ck_alerta_acao_tomada
        CHECK (
            acao_tomada IS NULL
            OR (
                status = 'RESOLVIDO'
                AND acao_tomada IN ('AJUSTOU', 'TRANSFERIU_LOTE', 'DESCARTOU')
            )
        );
