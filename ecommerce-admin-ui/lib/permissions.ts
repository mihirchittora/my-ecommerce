import type { AuthUser } from "@/lib/types";

export type Permission =
  | "CATALOG_READ"
  | "PRODUCT_READ" | "PRODUCT_CREATE" | "PRODUCT_UPDATE" | "PRODUCT_DELETE"
  | "CATEGORY_READ" | "CATEGORY_CREATE" | "CATEGORY_UPDATE" | "CATEGORY_DELETE"
  | "PRODUCT_IMAGE_UPLOAD" | "PRODUCT_IMAGE_DELETE"
  | "INVENTORY_READ" | "INVENTORY_RECEIVE" | "INVENTORY_ADJUST" | "INVENTORY_TRANSFER"
  | "INVENTORY_RESERVE" | "INVENTORY_CONFIRM" | "INVENTORY_RELEASE" | "INVENTORY_RECONCILE"
  | "INVENTORY_LOCATION_MANAGE" | "INVENTORY_UNIT_READ"
  | "USER_READ" | "USER_CREATE" | "USER_UPDATE" | "USER_ROLE_ASSIGN"
  | "ROLE_READ" | "ROLE_CREATE" | "PERMISSION_READ" | "ROLE_PERMISSION_UPDATE"
  | "ORDER_READ" | "ORDER_CREATE" | "ORDER_UPDATE" | "ORDER_CANCEL"
  | "CUSTOMER_READ" | "CUSTOMER_UPDATE" | "CART_READ";

export function hasPermission(user: AuthUser | null | undefined, permission: Permission) {
  return Boolean(user?.permissions.includes(permission));
}

export function hasAnyPermission(user: AuthUser | null | undefined, permissions: Permission[]) {
  return permissions.some((permission) => hasPermission(user, permission));
}

export function hasAllPermissions(user: AuthUser | null | undefined, permissions: Permission[]) {
  return permissions.every((permission) => hasPermission(user, permission));
}

export function isInternalUser(user: AuthUser | null | undefined) {
  if (!user) return false;
  return user.roles.some((role) => role !== "CUSTOMER") && user.permissions.some((permission) =>
    permission.startsWith("CATALOG_") || permission.startsWith("PRODUCT_") || permission.startsWith("CATEGORY_") ||
    permission.startsWith("INVENTORY_") || permission.startsWith("USER_") || permission.startsWith("ROLE_") ||
    permission.startsWith("PERMISSION_") || permission.startsWith("ORDER_") || permission.startsWith("CART_") ||
    permission.startsWith("CUSTOMER_"));
}

export function defaultRouteForUser(user: AuthUser | null | undefined) {
  if (hasAnyPermission(user, ["CATALOG_READ", "INVENTORY_READ", "ORDER_READ", "CART_READ"])) return "/dashboard";
  if (hasPermission(user, "CUSTOMER_READ")) return "/customers";
  if (hasPermission(user, "USER_READ")) return "/users";
  if (hasPermission(user, "USER_CREATE")) return "/users/new";
  if (hasPermission(user, "ROLE_READ")) return "/roles";
  if (hasPermission(user, "ROLE_CREATE")) return "/roles/new";
  if (hasPermission(user, "PERMISSION_READ")) return "/permissions";
  if (hasPermission(user, "CART_READ")) return "/carts";
  if (hasPermission(user, "ORDER_READ")) return "/orders";
  return "/dashboard";
}

export type RouteAccess = { allOf?: Permission[]; anyOf?: Permission[] };

export function routeAccess(pathname: string): RouteAccess {
  if (pathname === "/" || pathname === "/dashboard") return { anyOf: ["CATALOG_READ", "INVENTORY_READ", "ORDER_READ", "CART_READ"] };
  if (pathname === "/products/new") return { allOf: ["PRODUCT_CREATE"] };
  if (pathname === "/products" || pathname.startsWith("/products/")) return { allOf: ["PRODUCT_READ"] };
  if (pathname === "/categories") return { allOf: ["CATEGORY_READ"] };
  if (pathname === "/inventory/receive") return { allOf: ["INVENTORY_RECEIVE"] };
  if (pathname === "/inventory/units" || pathname.startsWith("/inventory/units/")) return { allOf: ["INVENTORY_UNIT_READ"] };
  if (pathname === "/inventory/stock" || pathname === "/inventory") return { allOf: ["INVENTORY_READ"] };
  if (pathname.startsWith("/inventory/stock/")) return { allOf: ["INVENTORY_READ", "INVENTORY_UNIT_READ"] };
  if (pathname === "/inventory/locations") return { allOf: ["INVENTORY_READ"] };
  if (pathname === "/inventory/reservations" || pathname.startsWith("/inventory/reservations/")) return { allOf: ["INVENTORY_READ"] };
  if (pathname === "/inventory/adjustments") return { allOf: ["INVENTORY_READ"] };
  if (pathname === "/inventory/transfers") return { allOf: ["INVENTORY_TRANSFER"] };
  if (pathname === "/inventory/reconciliation") return { allOf: ["INVENTORY_RECONCILE"] };
  if (pathname === "/orders" || pathname.startsWith("/orders/")) return { allOf: ["ORDER_READ"] };
  if (pathname === "/carts" || pathname.startsWith("/carts/")) return { allOf: ["CART_READ"] };
  if (pathname === "/customers" || pathname.startsWith("/customers/")) return { allOf: ["CUSTOMER_READ"] };
  if (pathname === "/users/new") return { allOf: ["USER_CREATE"] };
  if (pathname === "/users" || pathname.startsWith("/users/")) return { allOf: ["USER_READ"] };
  if (pathname === "/roles/new") return { allOf: ["ROLE_CREATE"] };
  if (pathname === "/roles" || pathname.startsWith("/roles/")) return { allOf: ["ROLE_READ"] };
  if (pathname === "/permissions") return { allOf: ["PERMISSION_READ"] };
  return {};
}
