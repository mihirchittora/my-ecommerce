"use client";

import Link from "next/link";
import { ShieldCheck, Users } from "lucide-react";
import { useAuth } from "@/components/auth-provider";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { DashboardMetricCard } from "@/components/dashboard/dashboard-metric-card";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAdminUsers, useAuthRoles } from "@/lib/api/auth-admin/queries";

export function AccessDashboardSection() {
  const { hasPermission } = useAuth();
  const canReadUsers = hasPermission("USER_READ");
  const canReadRoles = hasPermission("ROLE_READ");
  const users = useAdminUsers({ page: 0, size: 1 }, canReadUsers);
  const roles = useAuthRoles(canReadRoles);
  if (!canReadUsers && !canReadRoles) return null;
  return <section className="mt-6"><div className="mb-4 flex items-end justify-between"><div><p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">Auth service</p><h2 className="mt-1 text-lg font-semibold text-slate-900">Users & access</h2></div><Link href={canReadUsers ? "/users" : "/roles"} className="text-sm font-semibold text-primary hover:underline">Open access control</Link></div><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{canReadUsers && <DashboardMetricCard label="Total users" value={users.data?.totalElements ?? "—"} icon={Users} tone="blue" href="/users" />}{canReadRoles && <DashboardMetricCard label="Roles" value={roles.data?.length ?? "—"} icon={ShieldCheck} tone="violet" href="/roles" />}<DashboardMetricCard label="Status metrics" value="Not returned" icon={ShieldCheck} tone="amber" /></div><div className="mt-4">{canReadUsers && users.isLoading ? <LoadingCard rows={2} /> : canReadUsers && users.isError ? <ErrorState title="Access metrics unavailable" message="Auth could not provide the paginated user total." onRetry={() => void users.refetch()} /> : <Card><CardHeader><CardTitle>Access coverage</CardTitle></CardHeader><CardContent><p className="text-sm leading-6 text-slate-600">Auth returns total users and role definitions. Active-user, locked-user, privileged-user, last-login, and audit metrics are not exposed efficiently by the current API, so they are not calculated in the browser.</p></CardContent></Card>}</div></section>;
}
