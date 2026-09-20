INSERT INTO roles(name, description) VALUES
    ('CUSTOMER', 'Default role for registered customers'),
    ('CATALOG_ADMIN', 'Manages catalog products, categories, and images'),
    ('INVENTORY_ADMIN', 'Manages inventory operations and locations'),
    ('SUPER_ADMIN', 'All currently implemented permissions')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions(code, description) VALUES
    ('CATALOG_READ', 'Browse catalog resources'),
    ('PRODUCT_READ', 'Read products'),
    ('PRODUCT_CREATE', 'Create products'),
    ('PRODUCT_UPDATE', 'Update products'),
    ('PRODUCT_DELETE', 'Delete products'),
    ('CATEGORY_READ', 'Read categories'),
    ('CATEGORY_CREATE', 'Create categories'),
    ('CATEGORY_UPDATE', 'Update categories'),
    ('CATEGORY_DELETE', 'Delete categories'),
    ('PRODUCT_IMAGE_UPLOAD', 'Upload product images'),
    ('PRODUCT_IMAGE_DELETE', 'Delete product images'),
    ('INVENTORY_READ', 'Read aggregate inventory'),
    ('INVENTORY_RECEIVE', 'Receive stock'),
    ('INVENTORY_ADJUST', 'Adjust stock'),
    ('INVENTORY_TRANSFER', 'Transfer stock'),
    ('INVENTORY_RESERVE', 'Reserve stock'),
    ('INVENTORY_CONFIRM', 'Confirm reservations'),
    ('INVENTORY_RELEASE', 'Release reservations'),
    ('INVENTORY_RECONCILE', 'Reconcile inventory'),
    ('INVENTORY_LOCATION_MANAGE', 'Manage inventory locations'),
    ('INVENTORY_UNIT_READ', 'Read physical inventory units'),
    ('USER_READ', 'Read users'),
    ('USER_CREATE', 'Create users'),
    ('USER_UPDATE', 'Update users'),
    ('USER_ROLE_ASSIGN', 'Assign and remove user roles'),
    ('ROLE_READ', 'Read roles'),
    ('PERMISSION_READ', 'Read permissions'),
    ('ROLE_PERMISSION_UPDATE', 'Change role permissions'),
    ('ORDER_READ', 'Read orders (future domain)'),
    ('ORDER_CREATE', 'Create orders (future domain)'),
    ('ORDER_UPDATE', 'Update orders (future domain)'),
    ('ORDER_CANCEL', 'Cancel orders (future domain)'),
    ('CUSTOMER_READ', 'Read customer profiles (future domain)'),
    ('CUSTOMER_UPDATE', 'Update customer profiles (future domain)')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'CUSTOMER' AND p.code IN ('CATALOG_READ', 'PRODUCT_READ', 'CATEGORY_READ')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'CATALOG_ADMIN' AND p.code IN (
    'CATALOG_READ', 'PRODUCT_READ', 'PRODUCT_CREATE', 'PRODUCT_UPDATE', 'PRODUCT_DELETE',
    'CATEGORY_READ', 'CATEGORY_CREATE', 'CATEGORY_UPDATE', 'CATEGORY_DELETE',
    'PRODUCT_IMAGE_UPLOAD', 'PRODUCT_IMAGE_DELETE'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'INVENTORY_ADMIN' AND p.code IN (
    'INVENTORY_READ', 'INVENTORY_RECEIVE', 'INVENTORY_ADJUST', 'INVENTORY_TRANSFER',
    'INVENTORY_RESERVE', 'INVENTORY_CONFIRM', 'INVENTORY_RELEASE', 'INVENTORY_RECONCILE',
    'INVENTORY_LOCATION_MANAGE', 'INVENTORY_UNIT_READ'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.code NOT IN ('ORDER_READ', 'ORDER_CREATE', 'ORDER_UPDATE', 'ORDER_CANCEL', 'CUSTOMER_READ', 'CUSTOMER_UPDATE')
ON CONFLICT DO NOTHING;
