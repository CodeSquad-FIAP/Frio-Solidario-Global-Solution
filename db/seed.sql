-- FRIO SOLIDARIO - GLOBAL SOLUTION FIAP
-- Carga inicial para PostgreSQL
-- Executar depois do schema.sql

BEGIN;

-- 1. INSTITUICOES (3)

INSERT INTO instituicao
(id, nome, cnpj, tipo, cidade, endereco)
VALUES
(1, 'Banco de Alimentos Caminhos do Bem',
 '48302761000124', 'BANCO', 'São Paulo',
 'Rua das Acácias, 280 - Vila Mariana'),

(2, 'Mercado Bairro Solidário',
 '51628439000102', 'MERCADO', 'Cotia',
 'Avenida Professor Manoel José Pedroso, 1140 - Centro'),

(3, 'Padaria Sabor e Partilha',
 '64739215000101', 'PADARIA', 'Osasco',
 'Rua Dona Primitiva Vianco, 920 - Centro');


-- 2. USUARIOS (6)
-- Os hashes sao BCrypt (work factor 10) da senha demonstrativa documentada no
-- README, secao "Login (demo)". A senha em texto puro nao vai para o banco nem
-- para este arquivo: o login confere a senha contra o hash.

INSERT INTO usuario
(id, nome, email, senha_hash, papel, instituicao_id)
VALUES
(1, 'Mariana Costa',
 'mariana.costa@caminhosdobem.example',
 '$2a$10$uN9Au9Z/.N2nZ5dK46yjcuFF4Q7cQ.syE7akEKM.WGjgbwhgYl4Tq',
 'ADMIN', 1),

(2, 'Ana Rocha',
 'ana.rocha@caminhosdobem.example',
 '$2a$10$dwy4XginHJb/kvfwb9ODZ.bAlKnRbhH0Px1.ccFn/UJeCv8xqwa92',
 'VOLUNTARIO', 1),

(3, 'Rafael Nunes',
 'rafael.nunes@bairrosolidario.example',
 '$2a$10$AbGF5gV5h0SmBnCg5DmRfO2gJ5zMOFXckoYmNsphSP8IkgYdi67Q6',
 'ADMIN', 2),

(4, 'Bruna Oliveira',
 'bruna.oliveira@bairrosolidario.example',
 '$2a$10$5dIpsqDWT67pkbWH78eXWee7NzHckvk8GomMNbQq7fjxhlwsuiN/m',
 'VOLUNTARIO', 2),

(5, 'Carla Mendes',
 'carla.mendes@saborepartilha.example',
 '$2a$10$Jjs1gpnG1aCxue12uCRYnOrudH9SK.SSR21M8X4KYZE3zN5OdLVPq',
 'ADMIN', 3),

(6, 'Pedro Lima',
 'pedro.lima@saborepartilha.example',
 '$2a$10$4Zo5eCppv.d8X8CT8OLOnu7oLslc982aZAZP0aFXcAgUz0TLWzMKm',
 'VOLUNTARIO', 3);



-- 3. EQUIPAMENTOS (6)

INSERT INTO equipamento
(id, instituicao_id, nome, tipo,
 temp_min, temp_max, umid_max, ativo)
VALUES
(1, 1, 'Câmara fria de hortifrúti',
 'CAMARA', 0.00, 4.00, 85.00, TRUE),

(2, 1, 'Freezer de proteínas',
 'FREEZER', -25.00, -18.00, 90.00, TRUE),

(3, 2, 'Balcão refrigerado de doações',
 'GELADEIRA', 0.00, 5.00, 80.00, TRUE),

(4, 2, 'Freezer de congelados',
 'FREEZER', -25.00, -18.00, 85.00, TRUE),

(5, 3, 'Geladeira de excedentes',
 'GELADEIRA', 1.00, 6.00, 78.00, TRUE),

(6, 3, 'Câmara de resfriamento',
 'CAMARA', 0.00, 4.00, 80.00, TRUE);


-- 4. LEITURAS (30)
-- 5 leituras por equipamento.
-- Intervalo de 2 minutos.

INSERT INTO leitura
(id, equipamento_id, temperatura, umidade,
 porta_aberta, registrada_em, origem)
VALUES

-- EQUIPAMENTO 1: temperatura alta
(1, 1, 3.10, 71.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '8 minutes', 'SENSOR'),
(2, 1, 3.30, 72.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '6 minutes', 'SENSOR'),
(3, 1, 7.00, 73.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '4 minutes', 'SENSOR'),
(4, 1, 7.30, 74.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '2 minutes', 'SENSOR'),
(5, 1, 7.60, 75.00, FALSE,
 CURRENT_TIMESTAMP, 'SENSOR'),

-- EQUIPAMENTO 2: temperatura baixa
(6, 2, -21.80, 68.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '8 minutes', 'SENSOR'),
(7, 2, -22.20, 67.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '6 minutes', 'SENSOR'),
(8, 2, -26.20, 68.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '4 minutes', 'SENSOR'),
(9, 2, -26.40, 69.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '2 minutes', 'SENSOR'),
(10, 2, -26.60, 69.00, FALSE,
 CURRENT_TIMESTAMP, 'SENSOR'),

-- EQUIPAMENTO 3: porta aberta
(11, 3, 3.10, 70.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '8 minutes', 'SENSOR'),
(12, 3, 3.70, 71.00, TRUE,
 CURRENT_TIMESTAMP - INTERVAL '6 minutes', 'SENSOR'),
(13, 3, 3.50, 73.00, TRUE,
 CURRENT_TIMESTAMP - INTERVAL '4 minutes', 'SENSOR'),
(14, 3, 3.90, 74.00, TRUE,
 CURRENT_TIMESTAMP - INTERVAL '2 minutes', 'SENSOR'),
(15, 3, 4.10, 75.00, TRUE,
 CURRENT_TIMESTAMP, 'SENSOR'),

-- EQUIPAMENTO 4: funcionamento normal
(16, 4, -20.20, 75.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '8 minutes', 'SENSOR'),
(17, 4, -20.00, 75.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '6 minutes', 'SENSOR'),
(18, 4, -20.50, 76.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '4 minutes', 'SENSOR'),
(19, 4, -21.00, 75.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '2 minutes', 'SENSOR'),
(20, 4, -20.10, 75.00, FALSE,
 CURRENT_TIMESTAMP, 'SENSOR'),

-- EQUIPAMENTO 5: excesso de umidade
(21, 5, 3.00, 75.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '8 minutes', 'SIMULADO'),
(22, 5, 3.20, 76.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '6 minutes', 'SIMULADO'),
(23, 5, 3.10, 81.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '4 minutes', 'SIMULADO'),
(24, 5, 3.60, 83.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '2 minutes', 'SIMULADO'),
(25, 5, 3.80, 87.00, FALSE,
 CURRENT_TIMESTAMP, 'SIMULADO'),

-- EQUIPAMENTO 6: falha corrigida
(26, 6, 6.70, 76.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '8 minutes', 'SIMULADO'),
(27, 6, 6.90, 76.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '6 minutes', 'SIMULADO'),
(28, 6, 7.20, 77.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '4 minutes', 'SIMULADO'),
(29, 6, 3.40, 76.00, FALSE,
 CURRENT_TIMESTAMP - INTERVAL '2 minutes', 'SIMULADO'),
(30, 6, 3.00, 75.00, FALSE,
 CURRENT_TIMESTAMP, 'SIMULADO');


-- 5. ALERTAS (5)
-- 4 abertos e 1 resolvido

INSERT INTO alerta
(id, equipamento_id, leitura_id, tipo,
 severidade, status, criado_em,
 resolvido_em, resolvido_por_id)
VALUES
(1, 1, 5, 'TEMP_ALTA', 'CRITICA', 'ABERTO',
 CURRENT_TIMESTAMP, NULL, NULL),

(2, 2, 10, 'TEMP_BAIXA', 'ALTA', 'ABERTO',
 CURRENT_TIMESTAMP, NULL, NULL),

(3, 3, 15, 'PORTA_ABERTA', 'MEDIA', 'ABERTO',
 CURRENT_TIMESTAMP, NULL, NULL),

(4, 5, 25, 'UMIDADE', 'ALTA', 'ABERTO',
 CURRENT_TIMESTAMP, NULL, NULL),

(5, 6, 28, 'TEMP_ALTA', 'CRITICA', 'RESOLVIDO',
 CURRENT_TIMESTAMP - INTERVAL '4 minutes',
 CURRENT_TIMESTAMP - INTERVAL '1 minute', 6);


-- 6. LOTES (12)

INSERT INTO lote
(id, instituicao_id, descricao, categoria,
 kg, validade, status, retirado_em)
VALUES
(1, 1, 'Caixas de tomate italiano',
 'HORTIFRUTI', 120.50, CURRENT_DATE + 5,
 'DISTRIBUIDO', CURRENT_TIMESTAMP - INTERVAL '2 days'),

(2, 1, 'Maçãs para distribuição comunitária',
 'FRUTAS', 85.00, CURRENT_DATE + 9,
 'DISPONIVEL', NULL),

(3, 1, 'Peito de frango congelado',
 'PROTEINAS', 42.00, CURRENT_DATE + 75,
 'DISTRIBUIDO', CURRENT_TIMESTAMP - INTERVAL '6 days'),

(4, 1, 'Verduras de folhas variadas',
 'HORTIFRUTI', 18.50, CURRENT_DATE - 2,
 'PERDIDO', CURRENT_TIMESTAMP - INTERVAL '3 days'),

(5, 2, 'Iogurtes naturais embalados',
 'LATICINIOS', 36.00, CURRENT_DATE + 8,
 'DISPONIVEL', NULL),

(6, 2, 'Pacotes de legumes refrigerados',
 'HORTIFRUTI', 54.30, CURRENT_DATE + 6,
 'DISTRIBUIDO', CURRENT_TIMESTAMP - INTERVAL '1 day'),

(7, 2, 'Queijo minas em embalagens lacradas',
 'LATICINIOS', 21.80, CURRENT_DATE + 12,
 'PERDIDO', CURRENT_TIMESTAMP - INTERVAL '4 days'),

(8, 2, 'Porções de vegetais congelados',
 'CONGELADOS', 63.40, CURRENT_DATE + 90,
 'DISTRIBUIDO', CURRENT_TIMESTAMP - INTERVAL '12 days'),

(9, 3, 'Pães de forma embalados',
 'PANIFICADOS', 50.00, CURRENT_DATE + 4,
 'DISPONIVEL', NULL),

(10, 3, 'Bolos simples para centros sociais',
 'PANIFICADOS', 27.50, CURRENT_DATE + 3,
 'DISTRIBUIDO', CURRENT_TIMESTAMP - INTERVAL '1 day'),

(11, 3, 'Leite pasteurizado',
 'LATICINIOS', 25.00, CURRENT_DATE - 1,
 'PERDIDO', CURRENT_TIMESTAMP - INTERVAL '2 days'),

(12, 3, 'Pães integrais do turno da manhã',
 'PANIFICADOS', 41.00, CURRENT_DATE + 2,
 'DISTRIBUIDO', CURRENT_TIMESTAMP - INTERVAL '4 hours');


-- 7. ATUALIZAR SEQUENCIAS
-- Garante que novos IDs não sejam duplicados
-- quando a API cadastrar novos registros.

SELECT setval(
    pg_get_serial_sequence('instituicao', 'id'),
    (SELECT MAX(id) FROM instituicao)
);

SELECT setval(
    pg_get_serial_sequence('usuario', 'id'),
    (SELECT MAX(id) FROM usuario)
);

SELECT setval(
    pg_get_serial_sequence('equipamento', 'id'),
    (SELECT MAX(id) FROM equipamento)
);

SELECT setval(
    pg_get_serial_sequence('leitura', 'id'),
    (SELECT MAX(id) FROM leitura)
);

SELECT setval(
    pg_get_serial_sequence('alerta', 'id'),
    (SELECT MAX(id) FROM alerta)
);

SELECT setval(
    pg_get_serial_sequence('lote', 'id'),
    (SELECT MAX(id) FROM lote)
);

COMMIT;
