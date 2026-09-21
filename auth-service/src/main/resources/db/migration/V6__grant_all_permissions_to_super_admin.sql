-- SUPER_ADMIN must expose every currently seeded application capability,
-- including permissions added by later domain migrations.
UPDATE roles
SET description = 'All application roles and permissions'
WHERE name = 'SUPER_ADMIN';

INSERT INTO role_permissions(role_id, permission_id)
SELECT super_admin.id, permissions.id
FROM roles super_admin
CROSS JOIN permissions
WHERE super_admin.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;
