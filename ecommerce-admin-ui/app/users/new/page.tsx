"use client";

import Link from "next/link";
import { ArrowLeft, UserPlus } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { RoleChecklist } from "@/components/auth-admin/access-components";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { useAuth } from "@/components/auth-provider";
import { PageIntro } from "@/components/page-intro";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";
import { useAuthRoles, useCreateAdminUser } from "@/lib/api/auth-admin/queries";

function fieldError(error: unknown, field: string) {
  return error instanceof ApiError ? error.fieldErrors[field] : undefined;
}

export default function NewUserPage() {
  const router = useRouter();
  const { user, hasPermission } = useAuth();
  const roles = useAuthRoles(hasPermission("ROLE_READ"));
  const create = useCreateAdminUser();
  const [form, setForm] = useState({ firstName: "", lastName: "", email: "", password: "" });
  const [selectedRoleIds, setSelectedRoleIds] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const allowSuperAdmin = Boolean(user?.roles.includes("SUPER_ADMIN"));
  const toggleRole = (roleId: string) => setSelectedRoleIds((current) => current.includes(roleId) ? current.filter((id) => id !== roleId) : [...current, roleId]);
  const update = (field: keyof typeof form, value: string) => setForm((current) => ({ ...current, [field]: value }));
  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    if (form.password.length < 12) { setError("Password must be at least 12 characters."); return; }
    try {
      const created = await create.mutateAsync({ ...form, roleIds: selectedRoleIds });
      router.replace(`/users/${created.id}`);
    } catch (nextError) {
      setError(nextError instanceof ApiError ? nextError.message : "Unable to create this user.");
    }
  };

  return <><div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/users"><ArrowLeft className="h-4 w-4" />Back to users</Link></Button></div><PageIntro eyebrow="Users & access" title="Create user" description="Create an Auth identity with a verified email and optional initial roles. Auth owns password hashing and account security." /><form className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]" onSubmit={submit}><Card><CardHeader><CardTitle>Profile and credentials</CardTitle></CardHeader><CardContent className="space-y-5"><div className="grid gap-5 sm:grid-cols-2"><div><Label htmlFor="firstName">First name</Label><Input id="firstName" value={form.firstName} onChange={(event) => update("firstName", event.target.value)} required maxLength={80} className="mt-2" />{fieldError(create.error, "firstName") && <p className="mt-1 text-xs text-rose-600">{fieldError(create.error, "firstName")}</p>}</div><div><Label htmlFor="lastName">Last name</Label><Input id="lastName" value={form.lastName} onChange={(event) => update("lastName", event.target.value)} required maxLength={80} className="mt-2" />{fieldError(create.error, "lastName") && <p className="mt-1 text-xs text-rose-600">{fieldError(create.error, "lastName")}</p>}</div></div><div><Label htmlFor="email">Email</Label><Input id="email" type="email" value={form.email} onChange={(event) => update("email", event.target.value)} required maxLength={320} className="mt-2" />{fieldError(create.error, "email") && <p className="mt-1 text-xs text-rose-600">{fieldError(create.error, "email")}</p>}</div><div><Label htmlFor="password">Initial password</Label><Input id="password" type="password" value={form.password} onChange={(event) => update("password", event.target.value)} required minLength={12} maxLength={128} className="mt-2" /><p className="mt-1 text-xs text-slate-500">Auth accepts 12–128 characters and stores only its hash.</p></div>{error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}<div className="flex flex-wrap justify-end gap-3"><Button asChild variant="outline"><Link href="/users">Cancel</Link></Button><Button type="submit" disabled={create.isPending}><UserPlus className="h-4 w-4" />{create.isPending ? "Creating…" : "Create user"}</Button></div></CardContent></Card><Card><CardHeader><CardTitle>Initial roles</CardTitle><p className="text-sm text-muted-foreground">Permissions are derived from roles; direct permission assignment is not exposed.</p></CardHeader><CardContent>{roles.isLoading ? <LoadingCard rows={3} /> : roles.isError ? <ErrorState title="Roles unavailable" message="The user can still be created with Auth&apos;s default role." onRetry={() => void roles.refetch()} /> : roles.data ? <RoleChecklist roles={roles.data} selectedRoleIds={selectedRoleIds} onToggle={toggleRole} allowSuperAdmin={allowSuperAdmin} /> : <p className="text-sm text-slate-500">Auth did not return any roles. A user without selected roles receives Auth&apos;s default role.</p>}</CardContent></Card></form></>;
}

