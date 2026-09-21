INSERT INTO permissions(code, description)
VALUES ('ROLE_CREATE', 'Create authorization roles')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN' AND p.code = 'ROLE_CREATE'
ON CONFLICT DO NOTHING;
