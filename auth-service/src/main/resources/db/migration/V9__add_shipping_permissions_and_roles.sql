-- Shipping owns fulfillment, shipment, carrier, label, and tracking state.
-- These permissions are JWT claims; Shipping does not query auth_db per request.
INSERT INTO permissions(code, description) VALUES
    ('SHIPPING_READ', 'Read fulfillments, shipments, tracking, and history'),
    ('SHIPPING_CREATE', 'Create shipments after Order and Inventory validation'),
    ('SHIPPING_CANCEL', 'Cancel a shipment before carrier handoff'),
    ('SHIPPING_TRACK', 'Read shipment tracking information'),
    ('SHIPPING_LABEL_CREATE', 'Create or retrieve shipping labels'),
    ('SHIPPING_MANAGE', 'Manage shipping retries and operational recovery')
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles(name, description) VALUES
    ('SHIPPING_ADMIN', 'Manage shipping and fulfillment operations'),
    ('SHIPPING_OPERATIONS', 'Operate shipments, carriers, and tracking'),
    ('SHIPPING_READONLY', 'Read shipping and fulfillment operations')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SHIPPING_ADMIN'
  AND p.code IN ('SHIPPING_READ', 'SHIPPING_CREATE', 'SHIPPING_CANCEL', 'SHIPPING_TRACK', 'SHIPPING_LABEL_CREATE', 'SHIPPING_MANAGE')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SHIPPING_OPERATIONS'
  AND p.code IN ('SHIPPING_READ', 'SHIPPING_CREATE', 'SHIPPING_CANCEL', 'SHIPPING_TRACK', 'SHIPPING_LABEL_CREATE', 'SHIPPING_MANAGE')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SHIPPING_READONLY'
  AND p.code IN ('SHIPPING_READ', 'SHIPPING_TRACK')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT super_admin.id, p.id FROM roles super_admin CROSS JOIN permissions p
WHERE super_admin.name = 'SUPER_ADMIN'
  AND p.code LIKE 'SHIPPING_%'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles(user_id, role_id)
SELECT ur.user_id, shipping_role.id
FROM user_roles ur
JOIN roles super_admin ON super_admin.id = ur.role_id AND super_admin.name = 'SUPER_ADMIN'
CROSS JOIN roles shipping_role
WHERE shipping_role.name IN ('SHIPPING_ADMIN', 'SHIPPING_OPERATIONS', 'SHIPPING_READONLY')
ON CONFLICT DO NOTHING;
