-- Order operations are now part of the internal admin workspace.
-- Keep this additive so existing Auth databases receive the new permissions
-- without changing or removing any existing role assignments.
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.code IN ('ORDER_READ', 'ORDER_CREATE', 'ORDER_UPDATE', 'ORDER_CANCEL')
ON CONFLICT DO NOTHING;
