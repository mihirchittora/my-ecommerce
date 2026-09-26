"use client";

import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { useState } from "react";
import { authApi } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";

export default function ResetPasswordPage() {
  const params = useSearchParams(); const router = useRouter(); const [password, setPassword] = useState(""); const [confirmation, setConfirmation] = useState(""); const [error, setError] = useState(""); const [pending, setPending] = useState(false);
  async function submit(event: React.FormEvent<HTMLFormElement>) { event.preventDefault(); if (password.length < 12 || password !== confirmation) { setError(password.length < 12 ? "Password must be at least 12 characters." : "Passwords do not match."); return; } setPending(true); setError(""); try { await authApi.resetPassword(params.get("token") ?? "", password); router.replace("/login?reset=1"); } catch (caught) { setError(caught instanceof ApiError ? caught.message : "This reset link is invalid or expired."); } finally { setPending(false); } }
  return <div className="page-shell flex min-h-[70vh] items-center justify-center py-16"><Card className="w-full max-w-md p-7 md:p-9"><p className="eyebrow">Account recovery</p><h1 className="mt-3 font-display text-3xl font-bold">Set a new password</h1><form onSubmit={submit} className="mt-8 grid gap-5"><label><span className="field-label">New password</span><Input required minLength={12} type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label><label><span className="field-label">Confirm password</span><Input required minLength={12} type="password" value={confirmation} onChange={(event) => setConfirmation(event.target.value)} /></label>{error ? <p role="alert" className="text-sm text-red-700">{error}</p> : null}<Button type="submit" disabled={pending}>{pending ? "Updating…" : "Reset password"}</Button></form><Link className="mt-7 block text-center text-sm font-bold text-moss underline" href="/login">Back to sign in</Link></Card></div>;
}
