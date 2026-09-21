import type { PageResponse } from "@/lib/types";

export const USER_STATUSES = ["ACTIVE", "INACTIVE", "LOCKED", "PENDING"] as const;
export type UserStatus = (typeof USER_STATUSES)[number];
export type UserScope = "ALL" | "SERVICE";

export interface AdminUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  status: UserStatus;
  emailVerified: boolean;
  roles: string[];
}

export interface CreateUserPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  roleIds: string[];
}

export interface CreateRolePayload {
  name: string;
  description?: string;
}

export interface UpdateUserPayload {
  firstName: string;
  lastName: string;
}

export interface RolePermission {
  id: string;
  code: string;
  description: string | null;
}

export interface AuthRole {
  id: string;
  name: string;
  description: string | null;
  permissions: RolePermission[];
}

export interface PermissionDefinition {
  id: string;
  code: string;
  description: string | null;
}

export type AdminUserPage = PageResponse<AdminUser>;
