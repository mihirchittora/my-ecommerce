"use client";

import Link from "next/link";
import { useState } from "react";
import { authApi } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState(""); const [sent, setSent] = useState(false); const [error, setError] = useState(""); const [pending, setPending] = useState(false);
  async function submit(event: React.FormEvent<HTMLFormElement>) { event.preventDefault(); setPending(true); setError(""); try { await authApi.forgotPassword(email); setSent(true); } catch (caught) { setError(caught instanceof ApiError ? caught.message : "Please try again."); } finally { setPending(false); } }
  return <div className="page-shell flex min-h-[70vh] items-center justify-center py-16"><Card className="w-full max-w-md p-7 md:p-9"><p className="eyebrow">Account recovery</p><h1 className="mt-3 font-display text-3xl font-bold">Forgot your password?</h1>{sent ? <p className="mt-5 rounded-2xl bg-sage p-4 text-sm leading-6 text-moss">If an account exists, reset instructions have been sent.</p> : <form onSubmit={submit} className="mt-8 grid gap-5"><label><span className="field-label">Email</span><Input required type="email" value={email} onChange={(event) => setEmail(event.target.value)} /></label>{error ? <p role="alert" className="text-sm text-red-700">{error}</p> : null}<Button type="submit" disabled={pending}>{pending ? "Sending…" : "Send reset instructions"}</Button></form>}<Link className="mt-7 block text-center text-sm font-bold text-moss underline" href="/login">Back to sign in</Link></Card></div>;
}
