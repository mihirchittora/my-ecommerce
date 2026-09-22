"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { ApiError } from "@/lib/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";

export function AuthForm({ mode }: { mode: "login" | "register" }) {
  const isLogin = mode === "login";
  const { login, register } = useAuth();
  const router = useRouter();
  const params = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  const next = params.get("next")?.startsWith("/") ? params.get("next") as string : "/account";

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    if (!isLogin && password.length < 12) { setError("Password must be at least 12 characters."); return; }
    setPending(true);
    try {
      if (isLogin) await login(email, password);
      else await register({ email, password, firstName, lastName });
      router.replace(next);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "We could not complete that request. Please try again.");
    } finally { setPending(false); }
  }

  return <div className="page-shell flex min-h-[70vh] items-center justify-center py-16"><Card className="w-full max-w-md p-7 md:p-9"><p className="eyebrow">{isLogin ? "Welcome back" : "Join Morrow"}</p><h1 className="mt-3 font-display text-3xl font-bold tracking-tight">{isLogin ? "Sign in to your account" : "Create your account"}</h1><p className="mt-3 text-sm leading-6 text-ink/60">{isLogin ? "Pick up where you left off and keep your orders together." : "Save your details, track orders, and check out faster."}</p><form onSubmit={submit} className="mt-8 grid gap-5">{!isLogin ? <div className="grid gap-5 sm:grid-cols-2"><label><span className="field-label">First name</span><Input required value={firstName} onChange={(event) => setFirstName(event.target.value)} autoComplete="given-name" /></label><label><span className="field-label">Last name</span><Input required value={lastName} onChange={(event) => setLastName(event.target.value)} autoComplete="family-name" /></label></div> : null}<label><span className="field-label">Email</span><Input required type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" /></label><label><span className="field-label">Password</span><Input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete={isLogin ? "current-password" : "new-password"} minLength={isLogin ? undefined : 12} /></label>{error ? <p role="alert" className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-800">{error}</p> : null}<Button type="submit" disabled={pending}>{pending ? "Please wait…" : isLogin ? "Sign in" : "Create account"}</Button></form><p className="mt-7 text-center text-sm text-ink/60">{isLogin ? "New to Morrow?" : "Already have an account?"} <Link className="font-bold text-moss underline" href={isLogin ? `/register${next !== "/account" ? `?next=${encodeURIComponent(next)}` : ""}` : `/login${next !== "/account" ? `?next=${encodeURIComponent(next)}` : ""}`}>{isLogin ? "Create an account" : "Sign in"}</Link></p></Card></div>;
}
