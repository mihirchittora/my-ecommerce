import { authClient } from "@/lib/api/auth-admin/client";
import type { AdminUser, AdminUserPage, AuthRole, CreateRolePayload, CreateUserPayload, PermissionDefinition, UpdateUserPayload, UserScope, UserStatus } from "@/lib/api/auth-admin/types";

function query(values: Record<string, string | number | undefined>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== "") params.set(key, String(value));
  });
  const text = params.toString();
  return text ? `?${text}` : "";
}

export const authAdminApi = {
  listUsers: (params: { page: number; size: number; scope?: UserScope }) => authClient.request<AdminUserPage>(`/v1/users${query(params)}`),
  getUser: (id: string) => authClient.request<AdminUser>(`/v1/users/${encodeURIComponent(id)}`),
  createUser: (payload: CreateUserPayload) => authClient.json<AdminUser, CreateUserPayload>("/v1/users", payload),
  updateUser: (id: string, payload: UpdateUserPayload) => authClient.json<AdminUser, UpdateUserPayload>(`/v1/users/${encodeURIComponent(id)}`, payload, { method: "PUT" }),
  updateUserStatus: (id: string, status: UserStatus) => authClient.json<AdminUser, { status: UserStatus }>(`/v1/users/${encodeURIComponent(id)}/status`, { status }, { method: "PATCH" }),
  assignRole: (userId: string, roleId: string) => authClient.request<AdminUser>(`/v1/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(roleId)}`, { method: "POST" }),
  removeRole: (userId: string, roleId: string) => authClient.request<AdminUser>(`/v1/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(roleId)}`, { method: "DELETE" }),
  listRoles: () => authClient.request<AuthRole[]>("/v1/roles"),
  createRole: (payload: CreateRolePayload) => authClient.json<AuthRole, CreateRolePayload>("/v1/roles", payload),
  updateRolePermissions: (roleId: string, permissionIds: string[]) => authClient.json<AuthRole, { permissionIds: string[] }>(`/v1/roles/${encodeURIComponent(roleId)}/permissions`, { permissionIds }, { method: "PUT" }),
  listPermissions: () => authClient.request<PermissionDefinition[]>("/v1/permissions"),
};
