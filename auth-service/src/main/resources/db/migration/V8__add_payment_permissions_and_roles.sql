-- Payment management is an internal operations capability. Payment Service
-- remains the authority for payment state and enforces these permissions on its
-- admin endpoints.
INSERT INTO permissions(code, description) VALUES
    ('PAYMENT_READ', 'Read payment operations and provider references'),
    ('PAYMENT_REFUND', 'Issue full or partial payment refunds'),
    ('PAYMENT_RETRY', 'Retry failed payment attempts')
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles(name, description) VALUES
    ('PAYMENT_ADMIN', 'Manage payment operations, refunds, and retries'),
    ('PAYMENT_OPERATIONS', 'Operate payments, refunds, and retries'),
    ('PAYMENT_READONLY', 'Read payment operations')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'PAYMENT_ADMIN'
  AND p.code IN ('PAYMENT_READ', 'PAYMENT_REFUND', 'PAYMENT_RETRY')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'PAYMENT_OPERATIONS'
  AND p.code IN ('PAYMENT_READ', 'PAYMENT_REFUND', 'PAYMENT_RETRY')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'PAYMENT_READONLY'
  AND p.code = 'PAYMENT_READ'
ON CONFLICT DO NOTHING;

-- SUPER_ADMIN is system-managed and receives every implemented payment
-- permission just like the existing Catalog, Inventory, Order, and Cart scopes.
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.code IN ('PAYMENT_READ', 'PAYMENT_REFUND', 'PAYMENT_RETRY')
ON CONFLICT DO NOTHING;

-- Keep the role view consistent for existing Super Admin users as new payment
-- roles are introduced after the original role-seeding migrations ran.
INSERT INTO user_roles(user_id, role_id)
SELECT ur.user_id, payment_role.id
FROM user_roles ur
JOIN roles super_admin ON super_admin.id = ur.role_id AND super_admin.name = 'SUPER_ADMIN'
CROSS JOIN roles payment_role
WHERE payment_role.name IN ('PAYMENT_ADMIN', 'PAYMENT_OPERATIONS', 'PAYMENT_READONLY')
ON CONFLICT DO NOTHING;
