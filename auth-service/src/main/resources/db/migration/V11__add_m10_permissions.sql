INSERT INTO permissions(code, description) VALUES
    ('COUPON_READ', 'Read coupon and promotion configuration'),
    ('COUPON_MANAGE', 'Create, edit, activate, and deactivate coupons'),
    ('RETURN_READ', 'Read customer return requests'),
    ('RETURN_MANAGE', 'Approve, reject, receive, and complete returns'),
    ('REVIEW_READ', 'Read product reviews'),
    ('REVIEW_MODERATE', 'Approve and reject product reviews')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.code IN ('COUPON_READ', 'COUPON_MANAGE', 'RETURN_READ', 'RETURN_MANAGE', 'REVIEW_READ', 'REVIEW_MODERATE')
ON CONFLICT DO NOTHING;
