INSERT INTO permissions(code, description)
VALUES ('CART_READ', 'Read customer carts in support operations')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.code = 'CART_READ'
ON CONFLICT DO NOTHING;
