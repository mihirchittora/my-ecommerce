"use client";

import Link from "next/link";
import { Eye, Plus } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ApiError } from "@/lib/api/client";
import { useAuthRoles } from "@/lib/api/auth-admin/queries";
import { useAuth } from "@/components/auth-provider";

export default function RolesPage() {
  const roles = useAuthRoles();
  const { hasPermission } = useAuth();
  const canCreate = hasPermission("ROLE_CREATE");
  return <><PageIntro eyebrow="User Management" title="Roles" description="Roles are Auth-owned bundles of permissions. Create a role here, then assign its permissions and service users." action={canCreate ? { label: "Create role", href: "/roles/new" } : undefined} />{roles.isLoading ? <LoadingCard rows={5} /> : roles.isError ? <ErrorState title="Roles are unavailable" message={roles.error instanceof ApiError && roles.error.status === 403 ? "You do not have permission to view roles." : "Authentication service is temporarily unavailable."} onRetry={() => void roles.refetch()} /> : roles.data?.length === 0 ? <EmptyState title="No roles found" message="Auth did not return any seeded roles." action={canCreate ? <Button asChild><Link href="/roles/new"><Plus className="h-4 w-4" />Create role</Link></Button> : undefined} /> : roles.data && <Card className="overflow-hidden"><CardContent className="p-0"><div className="hidden overflow-x-auto md:block"><table className="w-full min-w-[760px] text-left"><caption className="sr-only">Auth roles</caption><thead><tr className="border-y border-slate-100 text-xs font-semibold uppercase tracking-[0.1em] text-slate-400"><th className="px-5 py-3">Role</th><th className="px-3 py-3">Description</th><th className="px-3 py-3">Permissions</th><th className="px-3 py-3">Assigned users</th><th className="px-3 py-3 text-right">Actions</th></tr></thead><tbody>{roles.data.map((role) => <tr className="table-row" key={role.id}><td className="px-5 py-4 font-semibold text-slate-800">{role.name}</td><td className="px-3 py-4 text-sm text-slate-600">{role.description || "—"}</td><td className="px-3 py-4"><Badge variant="muted">{role.permissions.length}</Badge></td><td className="px-3 py-4 text-sm text-slate-400">Not returned</td><td className="px-3 py-4 text-right"><Button asChild variant="ghost" size="sm"><Link href={`/roles/${role.id}`}><Eye className="h-4 w-4" />View</Link></Button></td></tr>)}</tbody></table></div><div className="divide-y divide-slate-100 md:hidden">{roles.data.map((role) => <article className="space-y-3 p-5" key={role.id}><div className="flex items-start justify-between gap-3"><p className="font-semibold text-slate-800">{role.name}</p><Badge variant="muted">{role.permissions.length} permissions</Badge></div><p className="text-sm text-slate-600">{role.description || "No description provided."}</p><p className="text-xs text-slate-400">Assigned users are not returned by Auth.</p><Button asChild variant="outline" size="sm"><Link href={`/roles/${role.id}`}><Eye className="h-4 w-4" />View role</Link></Button></article>)}</div></CardContent></Card>}</>;
}
