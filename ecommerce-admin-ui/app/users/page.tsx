"use client";

import Link from "next/link";
import { Eye, Plus, SearchX } from "lucide-react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback, useMemo } from "react";
import { AdminPagination } from "@/components/auth-admin/admin-pagination";
import { UserStatusBadge, RoleBadge } from "@/components/auth-admin/access-components";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ApiError } from "@/lib/api/client";
import { useServiceUsers } from "@/lib/api/auth-admin/queries";
import { useAuth } from "@/components/auth-provider";

const PAGE_SIZE = 20;

function pageFromQuery(value: string | null) {
  const page = Number(value);
  return Number.isInteger(page) && page >= 0 ? page : 0;
}

function errorMessage(error: unknown) {
  if (error instanceof ApiError && error.status === 403) return "You do not have permission to view Auth users.";
  if (error instanceof ApiError && (error.status === 0 || error.status >= 500)) return "Authentication service is temporarily unavailable.";
  return error instanceof ApiError ? error.message : "Authentication service is temporarily unavailable.";
}

export default function UsersPage() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const page = useMemo(() => pageFromQuery(searchParams.get("page")), [searchParams]);
  const users = useServiceUsers({ page, size: PAGE_SIZE });
  const { hasPermission } = useAuth();
  const updatePage = useCallback((nextPage: number) => { const params = new URLSearchParams(searchParams.toString()); if (nextPage > 0) params.set("page", String(nextPage)); else params.delete("page"); router.replace(`${pathname}${params.toString() ? `?${params}` : ""}`, { scroll: false }); }, [pathname, router, searchParams]);
  const canCreate = hasPermission("USER_CREATE");

  return <><PageIntro eyebrow="User Management" title="Service users" description="Manage internal Auth identities such as administrators, support agents, and service operators. Customer accounts are managed separately." action={canCreate ? { label: "Create service user", href: "/users/new" } : undefined} /><Card className="mb-5 border-slate-200 bg-slate-50"><CardContent className="flex gap-3 p-5"><SearchX className="mt-0.5 h-5 w-5 shrink-0 text-slate-500" /><p className="text-sm leading-6 text-slate-600">This view is limited to users with at least one non-customer role. Passwords, tokens, and security internals never enter this UI.</p></CardContent></Card>{users.isLoading ? <LoadingCard rows={7} /> : users.isError ? <ErrorState title="Service users are unavailable" message={errorMessage(users.error)} onRetry={() => void users.refetch()} /> : users.data?.content.length === 0 ? <EmptyState title="No service users found" message="Auth has not returned any internal user records for this page." action={canCreate ? <Button asChild><Link href="/users/new"><Plus className="h-4 w-4" />Create service user</Link></Button> : undefined} /> : users.data && <Card className="overflow-hidden"><CardContent className="p-0"><div className="hidden overflow-x-auto md:block"><table className="w-full min-w-[900px] text-left"><caption className="sr-only">Service users</caption><thead><tr className="border-y border-slate-100 text-xs font-semibold uppercase tracking-[0.1em] text-slate-400"><th className="px-5 py-3">Name</th><th className="px-3 py-3">Email</th><th className="px-3 py-3">Status</th><th className="px-3 py-3">Roles</th><th className="px-3 py-3">Email verified</th><th className="px-3 py-3 text-right">Actions</th></tr></thead><tbody>{users.data.content.map((user) => <tr className="table-row" key={user.id}><td className="px-5 py-4"><Link href={`/users/${user.id}`} className="font-semibold text-slate-800 hover:text-primary">{`${user.firstName} ${user.lastName}`.trim() || "Unnamed user"}</Link><p className="mt-1 font-mono text-[11px] text-slate-400">{user.id}</p></td><td className="px-3 py-4 text-sm text-slate-600">{user.email}</td><td className="px-3 py-4"><UserStatusBadge status={user.status} /></td><td className="px-3 py-4"><div className="flex max-w-sm flex-wrap gap-1.5">{user.roles.length ? user.roles.map((role) => <RoleBadge key={role} role={role} />) : <span className="text-sm text-slate-400">No roles</span>}</div></td><td className="px-3 py-4 text-sm text-slate-600">{user.emailVerified ? "Yes" : "No"}</td><td className="px-3 py-4 text-right"><Button asChild variant="ghost" size="sm"><Link href={`/users/${user.id}`}><Eye className="h-4 w-4" />View</Link></Button></td></tr>)}</tbody></table></div><div className="divide-y divide-slate-100 md:hidden">{users.data.content.map((user) => <article className="space-y-3 p-5" key={user.id}><div className="flex items-start justify-between gap-3"><Link href={`/users/${user.id}`} className="font-semibold text-slate-800 hover:text-primary">{`${user.firstName} ${user.lastName}`.trim() || "Unnamed user"}</Link><UserStatusBadge status={user.status} /></div><p className="break-all text-sm text-slate-600">{user.email}</p><div className="flex flex-wrap gap-1.5">{user.roles.map((role) => <RoleBadge key={role} role={role} />)}</div><Button asChild variant="outline" size="sm"><Link href={`/users/${user.id}`}><Eye className="h-4 w-4" />View user</Link></Button></article>)}</div><AdminPagination page={users.data} onPageChange={updatePage} /></CardContent></Card>}</>;
}
