"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, ShieldCheck, Sparkles } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useAuth } from "@/components/auth-provider";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";

const schema = z.object({ email: z.string().trim().email("Enter a valid email address."), password: z.string().min(1, "Password is required.") });
type Values = z.infer<typeof schema>;

function LoginContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { email: "", password: "" } });

  const submit = async (values: Values) => {
    setServerError(null);
    try {
      await login(values.email, values.password);
      const next = searchParams.get("next");
      router.replace(next?.startsWith("/") ? next : "/dashboard");
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : error instanceof Error ? error.message : "Unable to sign in. Please try again.");
    }
  };

  return <main className="min-h-screen bg-slate-950 px-4 py-10 sm:px-6"><div className="mx-auto grid min-h-[calc(100vh-5rem)] max-w-5xl items-center gap-8 lg:grid-cols-[1fr_420px]"><div className="hidden text-white lg:block"><div className="mb-6 flex items-center gap-3"><div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white text-slate-950"><Sparkles className="h-5 w-5" /></div><div><p className="font-semibold">Meridian</p><p className="text-xs uppercase tracking-[0.18em] text-slate-400">Admin operations</p></div></div><p className="max-w-xl text-5xl font-semibold leading-tight tracking-tight">A secure workspace for catalog and inventory operations.</p><p className="mt-6 max-w-lg text-base leading-7 text-slate-400">Sign in with an internal account to manage products, stock, physical units and operational workflows.</p><div className="mt-8 flex items-center gap-3 text-sm text-slate-300"><ShieldCheck className="h-5 w-5 text-blue-300" />Permission-based access is enforced by the platform services.</div></div><Card className="border-white/10 bg-white shadow-2xl"><CardHeader className="space-y-3"><div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-950 text-white lg:hidden"><Sparkles className="h-4 w-4" /></div><CardTitle className="text-2xl">Sign in to Meridian</CardTitle><p className="text-sm leading-6 text-muted-foreground">Use your internal admin or operations account.</p></CardHeader><CardContent><form className="space-y-5" onSubmit={form.handleSubmit(submit)} noValidate><div><Label htmlFor="login-email">Email</Label><Input id="login-email" className="mt-2" type="email" autoComplete="username" placeholder="admin@example.com" {...form.register("email")} />{form.formState.errors.email && <p className="mt-1.5 text-xs text-rose-600">{form.formState.errors.email.message}</p>}</div><div><Label htmlFor="login-password">Password</Label><Input id="login-password" className="mt-2" type="password" autoComplete="current-password" {...form.register("password")} />{form.formState.errors.password && <p className="mt-1.5 text-xs text-rose-600">{form.formState.errors.password.message}</p>}</div>{serverError && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2.5 text-sm text-rose-700">{serverError}</div>}<Button className="w-full" type="submit" disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Signing in…" : "Sign in"}<ArrowRight className="h-4 w-4" /></Button></form></CardContent></Card></div></main>;
}

export default function LoginPage() {
  return <Suspense fallback={<main className="min-h-screen bg-slate-950" />}><LoginContent /></Suspense>;
}
