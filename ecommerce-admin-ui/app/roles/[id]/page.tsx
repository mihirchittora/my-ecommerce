"use client";

import Link from "next/link";
import { ArrowLeft, Save } from "lucide-react";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { useAuth } from "@/components/auth-provider";
import { PageIntro } from "@/components/page-intro";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ApiError } from "@/lib/api/client";
import { useAuthPermissions, useAuthRoles, useUpdateRolePermissions } from "@/lib/api/auth-admin/queries";
import type { PermissionDefinition } from "@/lib/api/auth-admin/types";
import { titleCase } from "@/lib/utils";

function permissionDomain(code: string) {
  if (code.startsWith("CATALOG_") || code.startsWith("PRODUCT_") || code.startsWith("CATEGORY_")) return "Catalog";
  if (code.startsWith("INVENTORY_")) return "Inventory";
  if (code.startsWith("ORDER_")) return "Orders";
  if (code.startsWith("CART_")) return "Cart";
  if (code.startsWith("CUSTOMER_")) return "Customer";
  if (code.startsWith("PAYMENT_")) return "Payments";
  if (code.startsWith("USER_") || code.startsWith("ROLE_") || code.startsWith("PERMISSION_")) return "Users & Access";
  return "Other";
}

export default function RoleDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { hasPermission } = useAuth();
  const roles = useAuthRoles();
  const permissions = useAuthPermissions(hasPermission("PERMISSION_READ"));
  const update = useUpdateRolePermissions();
  const role = roles.data?.find((item) => item.id === id);
  const [selected, setSelected] = useState<string[]>([]);
  useEffect(() => { if (role) setSelected(role.permissions.map((permission) => permission.id)); }, [role]);
  const grouped = useMemo(() => { const groups = new Map<string, PermissionDefinition[]>(); (permissions.data ?? []).forEach((permission) => groups.set(permissionDomain(permission.code), [...(groups.get(permissionDomain(permission.code)) ?? []), permission])); return [...groups.entries()].sort(([left], [right]) => left.localeCompare(right)); }, [permissions.data]);
  if (roles.isLoading || (hasPermission("PERMISSION_READ") && permissions.isLoading)) return <LoadingCard rows={7} />;
  if (roles.isError) return <ErrorState title="Role unavailable" message={roles.error instanceof ApiError && roles.error.status === 403 ? "You do not have permission to view roles." : "Authentication service is temporarily unavailable."} onRetry={() => void roles.refetch()} />;
  if (!role) return <ErrorState title="Role not found" message="Auth did not return this role." onRetry={() => void roles.refetch()} />;
  const canEdit = role.name !== "SUPER_ADMIN" && hasPermission("ROLE_PERMISSION_UPDATE") && hasPermission("PERMISSION_READ") && !permissions.isError;
  const save = async () => { await update.mutateAsync({ roleId: id, permissionIds: selected }); router.refresh(); };
  return <><div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/roles"><ArrowLeft className="h-4 w-4" />Back to roles</Link></Button></div><PageIntro eyebrow="Users & access" title={role.name} description={role.description || "Auth role details and permission membership."} /><div className="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]"><Card><CardHeader><CardTitle>Role summary</CardTitle></CardHeader><CardContent className="space-y-4"><div><p className="text-xs uppercase tracking-[0.1em] text-slate-400">Role</p><p className="mt-1 text-lg font-semibold text-slate-800">{role.name}</p></div><div><p className="text-xs uppercase tracking-[0.1em] text-slate-400">Assigned users</p><p className="mt-1 text-sm text-slate-500">Auth does not expose a role-to-user lookup.</p></div><div><p className="text-xs uppercase tracking-[0.1em] text-slate-400">Current permissions</p><div className="mt-2 flex flex-wrap gap-2">{role.permissions.length ? role.permissions.map((permission) => <Badge variant="muted" key={permission.id}>{permission.code}</Badge>) : <span className="text-sm text-slate-500">No permissions</span>}</div></div></CardContent></Card><Card><CardHeader><CardTitle>Permissions</CardTitle><p className="text-sm text-muted-foreground">Only permissions returned by Auth are selectable. Changes replace the role&apos;s complete permission set.</p></CardHeader><CardContent>{permissions.isError ? <ErrorState title="Permission catalog unavailable" message="Role details are available, but editing requires PERMISSION_READ and the permission catalog." onRetry={() => void permissions.refetch()} /> : canEdit ? <div className="space-y-5">{grouped.map(([domain, values]) => <fieldset key={domain} className="rounded-xl border border-slate-100 p-4"><legend className="px-2 text-sm font-semibold text-slate-800">{titleCase(domain)}</legend><div className="grid gap-2 sm:grid-cols-2">{values.map((permission) => <label className="flex cursor-pointer items-start gap-3 rounded-lg p-2 hover:bg-slate-50" key={permission.id}><input type="checkbox" checked={selected.includes(permission.id)} onChange={() => setSelected((current) => current.includes(permission.id) ? current.filter((value) => value !== permission.id) : [...current, permission.id])} className="mt-1 h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary" /><span><span className="block text-sm font-medium text-slate-800">{permission.code}</span><span className="block text-xs leading-5 text-slate-500">{permission.description || "No description provided."}</span></span></label>)}</div></fieldset>)}<div className="flex justify-end"><Button onClick={() => void save()} disabled={update.isPending}><Save className="h-4 w-4" />{update.isPending ? "Saving…" : "Save permissions"}</Button></div>{update.error && <p role="alert" className="text-sm text-rose-600">{update.error instanceof ApiError ? update.error.message : "Unable to update role permissions."}</p>}</div> : <div className="space-y-3"><p className="text-sm text-slate-600">{role.name === "SUPER_ADMIN" ? "SUPER_ADMIN permissions are system-managed and include every seeded application permission." : "This role is read-only for the current operator. ROLE_PERMISSION_UPDATE and PERMISSION_READ are both required to edit the permission set."}</p><div className="flex flex-wrap gap-2">{role.permissions.map((permission) => <Badge variant="muted" key={permission.id}>{permission.code}</Badge>)}</div></div>}</CardContent></Card></div></>;
}
