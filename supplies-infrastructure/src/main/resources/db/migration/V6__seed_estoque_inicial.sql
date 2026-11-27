-- Seed idempotente em PostgreSQL e H2 (usado pelo smoke test de migrations).
INSERT INTO estoque (
    uuid,
    peca_uuid,
    saldo_atual,
    saldo_reservado,
    estoque_minimo,
    version,
    created_at,
    created_by
)
SELECT
    '55555555-5555-4555-8555-555555555555',
    '55555555-5555-5555-5555-555555555555',
    10,
    0,
    2,
    0,
    CURRENT_TIMESTAMP,
    'SEED'
WHERE NOT EXISTS (
    SELECT 1 FROM estoque WHERE peca_uuid = '55555555-5555-5555-5555-555555555555'
);

INSERT INTO estoque (
    uuid,
    peca_uuid,
    saldo_atual,
    saldo_reservado,
    estoque_minimo,
    version,
    created_at,
    created_by
)
SELECT
    '66666666-6666-4666-8666-666666666666',
    '66666666-6666-6666-6666-666666666666',
    5,
    0,
    3,
    0,
    CURRENT_TIMESTAMP,
    'SEED'
WHERE NOT EXISTS (
    SELECT 1 FROM estoque WHERE peca_uuid = '66666666-6666-6666-6666-666666666666'
);
