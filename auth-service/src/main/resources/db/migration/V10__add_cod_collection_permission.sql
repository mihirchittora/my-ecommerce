INSERT INTO permissions(code, description) VALUES
    ('PAYMENT_COLLECT', 'Confirm cash on delivery collection after delivery')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name IN ('SUPER_ADMIN', 'PAYMENT_ADMIN', 'PAYMENT_OPERATIONS')
  AND p.code = 'PAYMENT_COLLECT'
ON CONFLICT DO NOTHING;
