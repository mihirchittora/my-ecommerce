"use client";

import Link from "next/link";
import { ArrowLeft, ShieldPlus } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError } from "@/lib/api/client";
import { useCreateRole } from "@/lib/api/auth-admin/queries";
import { PageIntro } from "@/components/page-intro";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

export default function NewRolePage() {
  const router = useRouter();
  const create = useCreateRole();
  const [form, setForm] = useState({ name: "", description: "" });
  const [error, setError] = useState<string | null>(null);
  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    try {
      const role = await create.mutateAsync({ name: form.name.trim().toUpperCase(), description: form.description.trim() || undefined });
      router.replace(`/roles/${role.id}`);
    } catch (nextError) {
      setError(nextError instanceof ApiError ? nextError.message : "Unable to create this role.");
    }
  };
  return <><div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/roles"><ArrowLeft className="h-4 w-4" />Back to roles</Link></Button></div><PageIntro eyebrow="User Management" title="Create role" description="Create an empty Auth role, then choose its permissions from the role detail screen." /><form onSubmit={submit}><Card className="max-w-2xl"><CardHeader><CardTitle>Role details</CardTitle></CardHeader><CardContent className="space-y-5"><div><Label htmlFor="name">Role name</Label><Input id="name" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} placeholder="SUPPORT_AGENT" required maxLength={80} className="mt-2" /><p className="mt-1 text-xs text-slate-500">Use letters, numbers, and underscores. The value is stored in uppercase.</p></div><div><Label htmlFor="description">Description</Label><Textarea id="description" value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} placeholder="Handles customer support operations" maxLength={500} className="mt-2" /></div>{error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}<div className="flex justify-end gap-3"><Button asChild variant="outline"><Link href="/roles">Cancel</Link></Button><Button type="submit" disabled={create.isPending}><ShieldPlus className="h-4 w-4" />{create.isPending ? "Creating…" : "Create role"}</Button></div></CardContent></Card></form></>;
}
