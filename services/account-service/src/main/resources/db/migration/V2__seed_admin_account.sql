INSERT INTO accounts (
    id, username, email, password_hash, role, status, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    'admin',
    'admin@example.com',
    '$2a$10$3ex0l51qMpha7Btzho8xMuHfJsdDDpRXYvJh//x809z9Xni3PSpiq',
    'ADMIN',
    'ACTIVE',
    NOW(),
    NOW()
);


