"use client";

import Link from "next/link";
import { ArrowLeft, LockKeyhole, Pencil, ShieldPlus, ShieldMinus, UserRound } from "lucide-react";
import { useParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { EffectiveAccessPanel, RoleBadge, UserStatusBadge } from "@/components/auth-admin/access-components";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { useAuth } from "@/components/auth-provider";
import { PageIntro } from "@/components/page-intro";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";
import { useAdminUser, useAssignAdminRole, useAuthPermissions, useAuthRoles, useRemoveAdminRole, useUpdateAdminUser, useUpdateAdminUserStatus } from "@/lib/api/auth-admin/queries";
import type { UserStatus } from "@/lib/api/auth-admin/types";

function errorMessage(error: unknown) {
  if (error instanceof ApiError && error.status === 403) return "You do not have permission to view this Auth user.";
  if (error instanceof ApiError && error.status === 404) return "Auth user not found.";
  return error instanceof ApiError ? error.message : "Authentication service is temporarily unavailable.";
}

function nextStatus(status: UserStatus) {
  if (status === "ACTIVE") return "INACTIVE" as const;
  return "ACTIVE" as const;
}

export default function UserDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user: currentUser, hasPermission } = useAuth();
  const adminUser = useAdminUser(id);
  const roles = useAuthRoles(hasPermission("ROLE_READ"));
  const permissions = useAuthPermissions(hasPermission("PERMISSION_READ"));
  const update = useUpdateAdminUser();
  const status = useUpdateAdminUserStatus();
  const assign = useAssignAdminRole();
  const remove = useRemoveAdminRole();
  const [editing, setEditing] = useState(false);
  const [profile, setProfile] = useState({ firstName: "", lastName: "" });
  const [roleSearch, setRoleSearch] = useState("");
  const [roleError, setRoleError] = useState<string | null>(null);

  useEffect(() => { if (adminUser.data) setProfile({ firstName: adminUser.data.firstName, lastName: adminUser.data.lastName }); }, [adminUser.data]);
  const allowSuperAdmin = Boolean(currentUser?.roles.includes("SUPER_ADMIN"));
  const assignedRoleNames = useMemo(() => new Set(adminUser.data?.roles ?? []), [adminUser.data?.roles]);
  const availableRoles = roles.data?.filter((role) => !assignedRoleNames.has(role.name)).filter((role) => allowSuperAdmin || role.name !== "SUPER_ADMIN").filter((role) => `${role.name} ${role.description ?? ""}`.toLowerCase().includes(roleSearch.toLowerCase())) ?? [];

  if (adminUser.isLoading) return <LoadingCard rows={8} />;
  if (adminUser.isError || !adminUser.data) return <ErrorState title="User unavailable" message={errorMessage(adminUser.error)} onRetry={() => void adminUser.refetch()} />;

  const target = adminUser.data;
  const canUpdate = hasPermission("USER_UPDATE");
  const canAssignRoles = hasPermission("USER_ROLE_ASSIGN");
  const submitProfile = async (event: React.FormEvent<HTMLFormElement>) => { event.preventDefault(); await update.mutateAsync({ id, payload: profile }); setEditing(false); };
  const updateStatus = async (next: UserStatus) => { if (!window.confirm(`Change this user to ${next}?`)) return; await status.mutateAsync({ id, status: next }); };
  const assignRole = async (roleId: string, roleName: string) => { if (roleName === "SUPER_ADMIN" && !allowSuperAdmin) return; setRoleError(null); try { await assign.mutateAsync({ userId: id, roleId }); } catch (error) { setRoleError(error instanceof ApiError ? error.message : "Unable to assign this role."); } };
  const removeRole = async (roleId: string, roleName: string) => { if (roleName === "SUPER_ADMIN" && !allowSuperAdmin) return; if (!window.confirm(`Remove ${roleName} from this user?`)) return; setRoleError(null); try { await remove.mutateAsync({ userId: id, roleId }); } catch (error) { setRoleError(error instanceof ApiError ? error.message : "Unable to remove this role."); } };

  return <><div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/users"><ArrowLeft className="h-4 w-4" />Back to users</Link></Button></div><PageIntro eyebrow="Users & access" title={`${target.firstName} ${target.lastName}`.trim() || "Unnamed user"} description={target.email} /><div className="mb-8 flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-card sm:flex-row sm:items-center sm:justify-between"><div className="flex items-center gap-3"><UserRound className="h-5 w-5 text-primary" /><div><p className="text-xs uppercase tracking-[0.12em] text-slate-400">Account status</p><div className="mt-1"><UserStatusBadge status={target.status} /></div></div></div><div className="flex flex-wrap gap-2">{canUpdate && target.status !== "LOCKED" && <Button variant="outline" onClick={() => void updateStatus(nextStatus(target.status))}>{target.status === "ACTIVE" ? "Deactivate" : "Activate"}</Button>}{canUpdate && target.status !== "LOCKED" && <Button variant="destructive" onClick={() => void updateStatus("LOCKED")}><LockKeyhole className="h-4 w-4" />Lock</Button>}{canUpdate && target.status === "LOCKED" && <Button onClick={() => void updateStatus("ACTIVE")}>Unlock</Button>}{canUpdate && <Button variant="outline" onClick={() => setEditing((value) => !value)}><Pencil className="h-4 w-4" />{editing ? "Close edit" : "Edit profile"}</Button>}</div></div><div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]"><div className="space-y-6"><Card><CardHeader><CardTitle>Profile</CardTitle></CardHeader><CardContent>{editing ? <form className="space-y-4" onSubmit={(event) => void submitProfile(event)}><div><Label htmlFor="firstName">First name</Label><Input id="firstName" value={profile.firstName} onChange={(event) => setProfile((current) => ({ ...current, firstName: event.target.value }))} required maxLength={80} className="mt-2" /></div><div><Label htmlFor="lastName">Last name</Label><Input id="lastName" value={profile.lastName} onChange={(event) => setProfile((current) => ({ ...current, lastName: event.target.value }))} required maxLength={80} className="mt-2" /></div><div className="flex justify-end gap-2"><Button type="button" variant="outline" onClick={() => setEditing(false)}>Cancel</Button><Button type="submit" disabled={update.isPending}>{update.isPending ? "Saving…" : "Save changes"}</Button></div>{update.error && <p role="alert" className="text-sm text-rose-600">{update.error instanceof ApiError ? update.error.message : "Unable to update this user."}</p>}</form> : <dl className="grid gap-4 sm:grid-cols-2"><div><dt className="text-xs uppercase tracking-[0.1em] text-slate-400">First name</dt><dd className="mt-1 text-sm font-semibold text-slate-800">{target.firstName}</dd></div><div><dt className="text-xs uppercase tracking-[0.1em] text-slate-400">Last name</dt><dd className="mt-1 text-sm font-semibold text-slate-800">{target.lastName}</dd></div><div className="sm:col-span-2"><dt className="text-xs uppercase tracking-[0.1em] text-slate-400">Email</dt><dd className="mt-1 break-all text-sm font-semibold text-slate-800">{target.email}</dd></div><div><dt className="text-xs uppercase tracking-[0.1em] text-slate-400">Email verified</dt><dd className="mt-1 text-sm text-slate-700">{target.emailVerified ? "Yes" : "No"}</dd></div></dl>}</CardContent></Card><Card><CardHeader><CardTitle>Roles</CardTitle><p className="text-sm text-muted-foreground">Roles are the only source of effective permissions in this UI.</p></CardHeader><CardContent className="space-y-5"><div className="flex flex-wrap gap-2">{target.roles.length ? target.roles.map((role) => { const roleId = roles.data?.find((item) => item.name === role)?.id; const protectedRole = role === "SUPER_ADMIN" && !allowSuperAdmin; return <div className="flex items-center gap-1" key={role}><RoleBadge role={role} />{canAssignRoles && roleId && !protectedRole && <Button variant="ghost" size="icon-sm" aria-label={`Remove ${role}`} onClick={() => void removeRole(roleId, role)}><ShieldMinus className="h-4 w-4 text-rose-600" /></Button>}</div>; }) : <p className="text-sm text-slate-500">No roles assigned.</p>}</div>{canAssignRoles && hasPermission("ROLE_READ") && <div className="space-y-3 border-t border-slate-100 pt-5"><p className="text-sm font-semibold text-slate-800">Assign role</p><Input value={roleSearch} onChange={(event) => setRoleSearch(event.target.value)} placeholder="Search role by name or description" aria-label="Search roles to assign" />{availableRoles.length ? <div className="space-y-2">{availableRoles.map((role) => <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-100 p-3" key={role.id}><div><p className="text-sm font-semibold text-slate-800">{role.name}</p><p className="text-xs text-slate-500">{role.description || "No description provided."}</p></div><Button size="sm" variant="outline" disabled={assign.isPending} onClick={() => void assignRole(role.id, role.name)}><ShieldPlus className="h-4 w-4" />Assign</Button></div>)}</div> : <p className="text-sm text-slate-500">No unassigned roles match the search.</p>}</div>}{canAssignRoles && !hasPermission("ROLE_READ") && <p className="border-t border-slate-100 pt-4 text-sm text-slate-500">ROLE_READ is required to load role choices.</p>}{roleError && <p role="alert" className="text-sm text-rose-600">{roleError}</p>}{!allowSuperAdmin && <p className="text-xs leading-5 text-amber-700">SUPER_ADMIN assignment/removal is blocked in this UI because the backend does not yet enforce privileged-account safeguards. Auth must remain authoritative.</p>}</CardContent></Card><Card><CardHeader><CardTitle>Security and account status</CardTitle></CardHeader><CardContent className="space-y-3 text-sm text-slate-600"><p><strong className="text-slate-800">Status:</strong> {target.status}</p><p><strong className="text-slate-800">Email verification:</strong> {target.emailVerified ? "Verified" : "Not verified"}</p><p>Created-at, last-login, password, token, lock-history, and audit-event fields are not returned by the Auth API and are intentionally omitted.</p><p className="break-all font-mono text-xs text-slate-400">Auth user ID: {target.id}</p></CardContent></Card></div><div>{roles.isLoading ? <LoadingCard rows={6} /> : roles.isError ? <ErrorState title="Role permissions unavailable" message="The user profile is available, but effective permissions cannot be derived until Auth role metadata loads." onRetry={() => void roles.refetch()} /> : roles.data && <EffectiveAccessPanel user={target} roles={roles.data} permissions={permissions.data} />}</div></div></>;
}
