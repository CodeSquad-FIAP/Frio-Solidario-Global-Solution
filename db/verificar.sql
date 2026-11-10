-- FRIO SOLIDARIO - GLOBAL SOLUTION FIAP
-- Verificacao do banco PostgreSQL
-- Executar apos schema.sql e seed.sql

-- 1. VERIFICAR QUANTIDADE DE REGISTROS

DO $$
DECLARE
    total INTEGER;
BEGIN

    SELECT COUNT(*) INTO total FROM instituicao;

    IF total <> 3 THEN
        RAISE EXCEPTION 'Esperadas 3 instituicoes';
    END IF;

    SELECT COUNT(*) INTO total FROM usuario;

    IF total <> 6 THEN
        RAISE EXCEPTION 'Esperados 6 usuarios';
    END IF;

    SELECT COUNT(*) INTO total FROM equipamento;

    IF total <> 6 THEN
        RAISE EXCEPTION 'Esperados 6 equipamentos';
    END IF;

    SELECT COUNT(*) INTO total FROM leitura;

    IF total <> 30 THEN
        RAISE EXCEPTION 'Esperadas 30 leituras';
    END IF;

    SELECT COUNT(*) INTO total FROM alerta;

    IF total <> 5 THEN
        RAISE EXCEPTION 'Esperados 5 alertas';
    END IF;

    SELECT COUNT(*) INTO total FROM lote;

    IF total <> 12 THEN
        RAISE EXCEPTION 'Esperados 12 lotes';
    END IF;

    RAISE NOTICE 'OK: todas as quantidades estao corretas';

END;
$$;

-- 2. VERIFICAR INDICES OBRIGATORIOS

DO $$
DECLARE
    total INTEGER;
BEGIN

    SELECT COUNT(*) INTO total
    FROM pg_indexes
    WHERE schemaname = current_schema()
      AND indexname IN (
          'idx_leitura_equipamento_registrada_em',
          'idx_alerta_status'
      );

    IF total <> 2 THEN
        RAISE EXCEPTION 'Faltam indices obrigatorios';
    END IF;

    RAISE NOTICE 'OK: indices obrigatorios encontrados';

END;
$$;

-- 3. VERIFICAR RELACIONAMENTOS

DO $$
DECLARE
    total INTEGER;
BEGIN

    SELECT COUNT(*) INTO total
    FROM alerta a
    JOIN leitura l ON l.id = a.leitura_id
    WHERE a.equipamento_id <> l.equipamento_id;

    IF total <> 0 THEN
        RAISE EXCEPTION
            'Alerta associado ao equipamento errado';
    END IF;

    SELECT COUNT(*) INTO total
    FROM alerta
    WHERE status = 'ABERTO';

    IF total <> 4 THEN
        RAISE EXCEPTION 'Esperados 4 alertas abertos';
    END IF;

    SELECT COUNT(*) INTO total
    FROM alerta
    WHERE status = 'RESOLVIDO'
      AND resolvido_por_id IS NOT NULL
      AND resolvido_em IS NOT NULL;

    IF total <> 1 THEN
        RAISE EXCEPTION 'Esperado 1 alerta resolvido';
    END IF;

    RAISE NOTICE 'OK: relacionamentos e alertas corretos';

END;
$$;

-- 4. TESTAR CONSTRAINTS

DO $$
BEGIN

    -- Testar temperatura invalida
    BEGIN

        INSERT INTO equipamento
        (instituicao_id, nome, tipo,
         temp_min, temp_max, umid_max)
        VALUES
        (1, 'Equipamento invalido',
         'CAMARA', 8, 2, 80);

        RAISE EXCEPTION
            'Falha: temperatura invalida foi aceita';

    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE
            'OK: temperatura invalida bloqueada';
    END;


    -- Testar umidade invalida
    BEGIN

        INSERT INTO equipamento
        (instituicao_id, nome, tipo,
         temp_min, temp_max, umid_max)
        VALUES
        (1, 'Umidade invalida',
         'CAMARA', 0, 4, 120);

        RAISE EXCEPTION
            'Falha: umidade invalida foi aceita';

    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE
            'OK: umidade invalida bloqueada';
    END;


    -- Testar email duplicado
    BEGIN

        INSERT INTO usuario
        (nome, email, senha_hash, papel, instituicao_id)
        VALUES
        ('Usuario duplicado',
         'ana.rocha@caminhosdobem.example',
         'hash_ficticio', 'VOLUNTARIO', 1);

        RAISE EXCEPTION
            'Falha: email duplicado foi aceito';

    EXCEPTION WHEN unique_violation THEN
        RAISE NOTICE
            'OK: email duplicado bloqueado';
    END;


    -- Testar papel invalido
    BEGIN

        INSERT INTO usuario
        (nome, email, senha_hash, papel, instituicao_id)
        VALUES
        ('Usuario invalido',
         'papel.invalido@example.org',
         'hash_ficticio', 'GERENTE', 1);

        RAISE EXCEPTION
            'Falha: papel invalido foi aceito';

    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE
            'OK: papel invalido bloqueado';
    END;


    -- Testar status invalido do lote
    BEGIN

        INSERT INTO lote
        (instituicao_id, descricao,
         categoria, kg, validade, status)
        VALUES
        (1, 'Lote invalido',
         'FRUTAS', 20, CURRENT_DATE, 'RESERVADO');

        RAISE EXCEPTION
            'Falha: status invalido foi aceito';

    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE
            'OK: status invalido bloqueado';
    END;


    -- Testar lote DISTRIBUIDO sem data de retirada
    BEGIN

        INSERT INTO lote
        (instituicao_id, descricao,
         categoria, kg, validade, status)
        VALUES
        (1, 'Lote distribuido sem retirada',
         'FRUTAS', 10, CURRENT_DATE + 3, 'DISTRIBUIDO');

        RAISE EXCEPTION
            'Falha: lote distribuido sem retirada foi aceito';

    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE
            'OK: lote distribuido sem retirada bloqueado';
    END;


    -- Testar lote DISPONIVEL com data de retirada
    BEGIN

        INSERT INTO lote
        (instituicao_id, descricao,
         categoria, kg, validade, status, retirado_em)
        VALUES
        (1, 'Lote disponivel ja retirado',
         'FRUTAS', 10, CURRENT_DATE + 3,
         'DISPONIVEL', CURRENT_TIMESTAMP);

        RAISE EXCEPTION
            'Falha: lote disponivel com retirada foi aceito';

    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE
            'OK: lote disponivel com retirada bloqueado';
    END;


    -- Testar chave estrangeira invalida
    BEGIN

        INSERT INTO leitura
        (equipamento_id, temperatura,
         umidade, porta_aberta, origem)
        VALUES
        (99999, 3.5, 70, FALSE, 'SENSOR');

        RAISE EXCEPTION
            'Falha: equipamento inexistente foi aceito';

    EXCEPTION WHEN foreign_key_violation THEN
        RAISE NOTICE
            'OK: chave estrangeira invalida bloqueada';
    END;

    RAISE NOTICE
        'SUCESSO: principais constraints verificadas';

END;
$$;


-- 5. RESUMO FINAL DO BANCO

SELECT
    'instituicao' AS entidade,
    COUNT(*) AS registros
FROM instituicao

UNION ALL

SELECT 'usuario', COUNT(*)
FROM usuario

UNION ALL

SELECT 'equipamento', COUNT(*)
FROM equipamento

UNION ALL

SELECT 'leitura', COUNT(*)
FROM leitura

UNION ALL

SELECT 'alerta', COUNT(*)
FROM alerta

UNION ALL

SELECT 'lote', COUNT(*)
FROM lote

ORDER BY entidade;
