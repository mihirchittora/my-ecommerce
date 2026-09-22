"use client";

import { KeyRound } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ErrorState, EmptyState, LoadingCard } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";
import { ApiError } from "@/lib/api/client";
import { useAuthPermissions } from "@/lib/api/auth-admin/queries";

function domain(code: string) {
  if (code.startsWith("CATALOG_") || code.startsWith("PRODUCT_") || code.startsWith("CATEGORY_")) return "Catalog";
  if (code.startsWith("INVENTORY_")) return "Inventory";
  if (code.startsWith("ORDER_")) return "Orders";
  if (code.startsWith("CART_")) return "Cart";
  if (code.startsWith("CUSTOMER_")) return "Customer";
  if (code.startsWith("PAYMENT_")) return "Payments";
  if (code.startsWith("USER_") || code.startsWith("ROLE_") || code.startsWith("PERMISSION_")) return "Users & Access";
  return "Other";
}

export default function PermissionsPage() {
  const permissions = useAuthPermissions();
  const groups = new Map<string, NonNullable<typeof permissions.data>>();
  (permissions.data ?? []).forEach((permission) => groups.set(domain(permission.code), [...(groups.get(domain(permission.code)) ?? []), permission]));
  return <><PageIntro eyebrow="Users & access" title="Permissions" description="This is the static permission catalog owned by Auth. Permissions are read-only here; role membership is managed from Roles." />{permissions.isLoading ? <LoadingCard rows={7} /> : permissions.isError ? <ErrorState title="Permissions are unavailable" message={permissions.error instanceof ApiError && permissions.error.status === 403 ? "You do not have permission to view permissions." : "Authentication service is temporarily unavailable."} onRetry={() => void permissions.refetch()} /> : permissions.data?.length === 0 ? <EmptyState title="No permissions found" message="Auth did not return any permission definitions." /> : <div className="grid gap-5 lg:grid-cols-2">{[...groups.entries()].sort(([left], [right]) => left.localeCompare(right)).map(([name, values]) => <Card key={name}><CardHeader><div className="flex items-center gap-2"><KeyRound className="h-5 w-5 text-primary" /><CardTitle>{name}</CardTitle></div><p className="text-sm text-muted-foreground">{values.length} permission{values.length === 1 ? "" : "s"} returned by Auth</p></CardHeader><CardContent className="space-y-2">{values.sort((left, right) => left.code.localeCompare(right.code)).map((permission) => <div className="rounded-xl border border-slate-100 p-3" key={permission.id}><p className="font-mono text-sm font-semibold text-slate-800">{permission.code}</p><p className="mt-1 text-sm leading-5 text-slate-500">{permission.description || "No description provided."}</p></div>)}</CardContent></Card>)}</div>}</>;
}
