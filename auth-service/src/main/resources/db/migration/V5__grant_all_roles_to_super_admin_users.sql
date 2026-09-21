-- SUPER_ADMIN is the full application administrator. Keep every current
-- application role attached to any user carrying SUPER_ADMIN so role-based
-- views and future role-aware policies see the complete administrative scope.
INSERT INTO user_roles(user_id, role_id)
SELECT user_roles.user_id, application_roles.id
FROM user_roles
JOIN roles super_admin_role ON super_admin_role.id = user_roles.role_id
CROSS JOIN roles application_roles
WHERE super_admin_role.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;
