import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { authAdminApi } from "@/lib/api/auth-admin/admin";
import type { AuthRole, CreateUserPayload, UpdateUserPayload, UserScope, UserStatus } from "@/lib/api/auth-admin/types";

export const authAdminQueryKeys = {
  users: ["auth-admin", "users"] as const,
  user: (id: string) => ["auth-admin", "users", id] as const,
  roles: ["auth-admin", "roles"] as const,
  permissions: ["auth-admin", "permissions"] as const,
};

export function useAdminUsers(params: { page: number; size: number; scope?: UserScope }, enabled = true) {
  return useQuery({ queryKey: [...authAdminQueryKeys.users, params], queryFn: () => authAdminApi.listUsers(params), enabled, placeholderData: (previous) => previous });
}

export function useServiceUsers(params: { page: number; size: number }, enabled = true) {
  return useAdminUsers({ ...params, scope: "SERVICE" }, enabled);
}

export function useAdminUser(id: string, enabled = true) {
  return useQuery({ queryKey: authAdminQueryKeys.user(id), queryFn: () => authAdminApi.getUser(id), enabled: Boolean(id) && enabled });
}

export function useAuthRoles(enabled = true) {
  return useQuery({ queryKey: authAdminQueryKeys.roles, queryFn: authAdminApi.listRoles, enabled, staleTime: 60_000 });
}

export function useAuthPermissions(enabled = true) {
  return useQuery({ queryKey: authAdminQueryKeys.permissions, queryFn: authAdminApi.listPermissions, enabled, staleTime: 60_000 });
}

export function useCreateAdminUser() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: (payload: CreateUserPayload) => authAdminApi.createUser(payload), onSuccess: () => { void queryClient.invalidateQueries({ queryKey: authAdminQueryKeys.users }); } });
}

export function useCreateRole() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ name, description }: { name: string; description?: string }) => authAdminApi.createRole({ name, description }), onSuccess: () => { void queryClient.invalidateQueries({ queryKey: authAdminQueryKeys.roles }); } });
}

export function useUpdateAdminUser() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ id, payload }: { id: string; payload: UpdateUserPayload }) => authAdminApi.updateUser(id, payload), onSuccess: (user) => { queryClient.setQueryData(authAdminQueryKeys.user(user.id), user); void queryClient.invalidateQueries({ queryKey: authAdminQueryKeys.users }); } });
}

export function useUpdateAdminUserStatus() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ id, status }: { id: string; status: UserStatus }) => authAdminApi.updateUserStatus(id, status), onSuccess: (user) => { queryClient.setQueryData(authAdminQueryKeys.user(user.id), user); void queryClient.invalidateQueries({ queryKey: authAdminQueryKeys.users }); } });
}

export function useAssignAdminRole() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ userId, roleId }: { userId: string; roleId: string }) => authAdminApi.assignRole(userId, roleId), onSuccess: (user) => { queryClient.setQueryData(authAdminQueryKeys.user(user.id), user); void queryClient.invalidateQueries({ queryKey: authAdminQueryKeys.users }); } });
}

export function useRemoveAdminRole() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ userId, roleId }: { userId: string; roleId: string }) => authAdminApi.removeRole(userId, roleId), onSuccess: (user) => { queryClient.setQueryData(authAdminQueryKeys.user(user.id), user); void queryClient.invalidateQueries({ queryKey: authAdminQueryKeys.users }); } });
}

export function useUpdateRolePermissions() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ roleId, permissionIds }: { roleId: string; permissionIds: string[] }) => authAdminApi.updateRolePermissions(roleId, permissionIds), onSuccess: (role) => { queryClient.setQueryData(authAdminQueryKeys.roles, (roles: AuthRole[] | undefined) => roles?.map((item) => item.id === role.id ? role : item)); } });
}
