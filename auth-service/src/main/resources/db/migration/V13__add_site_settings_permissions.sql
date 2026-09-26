INSERT INTO permissions(code, description) VALUES
    ('SITE_SETTINGS_READ', 'Read storefront branding and carousel settings'),
    ('SITE_SETTINGS_UPDATE', 'Update storefront branding and carousel settings')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name IN ('CATALOG_ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('SITE_SETTINGS_READ', 'SITE_SETTINGS_UPDATE')
ON CONFLICT DO NOTHING;
