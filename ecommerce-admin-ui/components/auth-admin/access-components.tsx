import { Check, ChevronDown, ChevronRight, ShieldAlert, X } from "lucide-react";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { AdminUser, AuthRole, PermissionDefinition, UserStatus } from "@/lib/api/auth-admin/types";
import { effectivePermissions, serviceAccessSummary } from "@/lib/access-model";
import { titleCase } from "@/lib/utils";

export function UserStatusBadge({ status }: { status: UserStatus }) {
  const variant = status === "ACTIVE" ? "success" : status === "LOCKED" ? "danger" : status === "PENDING" ? "warning" : "muted";
  return <Badge variant={variant}>{titleCase(status)}</Badge>;
}

export function RoleBadge({ role }: { role: string }) {
  return <Badge variant={role === "SUPER_ADMIN" ? "danger" : "outline"}>{role}</Badge>;
}

export function EffectiveAccessPanel({ user, roles, permissions }: { user: AdminUser; roles: AuthRole[]; permissions?: PermissionDefinition[] }) {
  const [expanded, setExpanded] = useState<string | null>(null);
  const effective = effectivePermissions(user.roles, roles);
  const services = serviceAccessSummary(effective);
  const knownPermissionCodes = new Set(permissions?.map((permission) => permission.code));
  return <div className="space-y-6"><Card><CardHeader><CardTitle>Effective permissions</CardTitle><p className="text-sm text-muted-foreground">Derived from the user&apos;s assigned roles and the permissions returned by Auth.</p></CardHeader><CardContent>{effective.length ? <div className="flex flex-wrap gap-2">{effective.map((permission) => <Badge key={permission.code} variant="muted" title={permission.description ?? undefined}>{permission.code}</Badge>)}</div> : <p className="text-sm text-slate-500">No effective permissions were returned for this user&apos;s roles.</p>}</CardContent></Card><Card><CardHeader><CardTitle>Service access</CardTitle><p className="text-sm text-muted-foreground">A service is available only when at least one actual permission maps to its domain.</p></CardHeader><CardContent className="space-y-3">{["Catalog", "Inventory", "Orders", "Cart", "Customer", "Users & Access"].map((service) => { const entry = services.find((item) => item.service === service); const isOpen = expanded === service; return <div className="rounded-xl border border-slate-100" key={service}><button type="button" className="flex w-full items-center justify-between gap-3 p-4 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" aria-expanded={isOpen} onClick={() => setExpanded(isOpen ? null : service)}><span className="flex items-center gap-2 text-sm font-semibold text-slate-800">{entry ? <Check className="h-4 w-4 text-emerald-600" /> : <X className="h-4 w-4 text-slate-300" />}{service}</span><span className="flex items-center gap-2 text-xs text-slate-500">{entry ? `${entry.permissions.length} permission${entry.permissions.length === 1 ? "" : "s"}` : "No access"}{isOpen ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}</span></button>{isOpen && <div className="border-t border-slate-100 p-4">{entry ? <div className="flex flex-wrap gap-2">{entry.permissions.filter((permission) => !permissions || knownPermissionCodes.has(permission.code)).map((permission) => <Badge key={permission.code} variant="muted" title={permission.description ?? undefined}>{permission.code}</Badge>)}</div> : <p className="text-sm text-slate-500">No permission returned for this service.</p>}</div>}</div>; })}</CardContent></Card><Card className="border-slate-200 bg-slate-50"><CardContent className="flex gap-3 p-5"><ShieldAlert className="mt-0.5 h-5 w-5 shrink-0 text-slate-500" /><p className="text-sm leading-6 text-slate-600">Auth does not expose last-login, account-created, audit-event, or privileged-account safeguard endpoints in the current contract. Those fields are intentionally not inferred or displayed as if they were available.</p></CardContent></Card></div>;
}

export function RoleChecklist({ roles, selectedRoleIds, onToggle, allowSuperAdmin }: { roles: AuthRole[]; selectedRoleIds: string[]; onToggle: (roleId: string) => void; allowSuperAdmin: boolean }) {
  const [search, setSearch] = useState("");
  const visible = roles.filter((role) => allowSuperAdmin || role.name !== "SUPER_ADMIN").filter((role) => `${role.name} ${role.description ?? ""}`.toLowerCase().includes(search.toLowerCase()));
  return <div className="space-y-3"><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search roles" aria-label="Search roles" className="flex h-10 w-full rounded-xl border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/20" />{visible.length ? <div className="grid gap-2">{visible.map((role) => { const selected = selectedRoleIds.includes(role.id); return <label key={role.id} className="flex cursor-pointer items-start gap-3 rounded-xl border border-slate-100 p-3 hover:bg-slate-50"><input type="checkbox" checked={selected} onChange={() => onToggle(role.id)} className="mt-1 h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary" /><span><span className="block text-sm font-semibold text-slate-800">{role.name}</span><span className="mt-1 block text-xs leading-5 text-slate-500">{role.description || "No description provided."}</span></span></label>; })}</div> : <p className="text-sm text-slate-500">No roles match this search.</p>}{!allowSuperAdmin && <p className="text-xs leading-5 text-amber-700">SUPER_ADMIN is not offered because the current Auth contract does not provide safe privileged-account enforcement.</p>}</div>;
}

