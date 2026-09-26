export interface AccessRole {
  name: string;
  permissions: Array<{ code: string; description: string | null }>;
}

export interface EffectivePermission {
  code: string;
  description: string | null;
}

export interface ServiceAccess {
  service: string;
  permissions: EffectivePermission[];
}

export function effectivePermissions(roleNames: string[], roles: AccessRole[]): EffectivePermission[] {
  const permissions = new Map<string, EffectivePermission>();
  roles.filter((role) => roleNames.includes(role.name)).forEach((role) => role.permissions.forEach((permission) => permissions.set(permission.code, permission)));
  return [...permissions.values()].sort((left, right) => left.code.localeCompare(right.code));
}

function serviceForPermission(code: string) {
  if (code.startsWith("SITE_SETTINGS_")) return "Storefront";
  if (code.startsWith("CATALOG_") || code.startsWith("PRODUCT_") || code.startsWith("CATEGORY_")) return "Catalog";
  if (code.startsWith("INVENTORY_")) return "Inventory";
  if (code.startsWith("ORDER_")) return "Orders";
  if (code.startsWith("CART_")) return "Cart";
  if (code.startsWith("CUSTOMER_")) return "Customer";
  if (code.startsWith("PAYMENT_")) return "Payments";
  if (code.startsWith("COUPON_") || code.startsWith("RETURN_") || code.startsWith("REVIEW_")) return "Commerce";
  if (code.startsWith("SHIPPING_")) return "Shipping";
  if (code.startsWith("USER_") || code.startsWith("ROLE_") || code.startsWith("PERMISSION_")) return "Users & Access";
  return "Other";
}

export function serviceAccessSummary(permissions: EffectivePermission[]): ServiceAccess[] {
  const grouped = new Map<string, EffectivePermission[]>();
  permissions.forEach((permission) => {
    const service = serviceForPermission(permission.code);
    grouped.set(service, [...(grouped.get(service) ?? []), permission]);
  });
  return [...grouped.entries()].sort(([left], [right]) => left.localeCompare(right)).map(([service, servicePermissions]) => ({ service, permissions: servicePermissions }));
}
