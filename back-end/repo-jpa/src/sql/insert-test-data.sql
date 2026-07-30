
INSERT INTO users (username, password, role, created_at)
VALUES ('alice', 'hashed_password_alice', 'ADMIN', 1700000000000),
       ('bob', 'hashed_password_bob', 'JUDGE', 1700000001000),
       ('carlos', 'hashed_password_carlos', 'JUDGE', 1700000002000);

INSERT INTO tokens (token_validation, user_id, created_at, last_used_at)
VALUES
    -- alice (id=1)
    ('token-alice-1', 1, 1700000010000, 1700000010000),
    ('token-alice-2', 1, 1700000011000, 1700000011000),
    ('token-alice-3', 1, 1700000012000, 1700000012000),
    -- bob (id=2)
    ('token-bob-1', 2, 1700000020000, 1700000020000),
    ('token-bob-2', 2, 1700000021000, 1700000021000),
    ('token-bob-3', 2, 1700000022000, 1700000022000),
    -- carlos (id=3)
    ('token-carlos-1', 3, 1700000030000, 1700000030000),
    ('token-carlos-2', 3, 1700000031000, 1700000031000),
    ('token-carlos-3', 3, 1700000032000, 1700000032000);

INSERT INTO clubs(name, short_name, created_at)
VALUES
    ('BG BARS TEAM', 'BG BARS', 1700000030000);