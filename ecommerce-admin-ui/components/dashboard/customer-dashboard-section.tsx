"use client";

import Link from "next/link";
import { ArrowUpRight, CheckCircle2, KeyRound, ShieldCheck, UserCog, Users } from "lucide-react";
import { useAuth } from "@/components/auth-provider";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function CustomerDashboardSection() {
  const { hasPermission } = useAuth();
  if (!hasPermission("CUSTOMER_READ")) return null;
  const links = [
    { href: "/customers", label: "Customers", description: "Profiles and shipping/billing addresses", icon: Users, permission: "CUSTOMER_READ" as const },
    { href: "/users", label: "Service users", description: "Administrators and service operators", icon: UserCog, permission: "USER_READ" as const },
    { href: "/roles", label: "Roles", description: "Role definitions and permissions", icon: ShieldCheck, permission: "ROLE_READ" as const },
    { href: "/permissions", label: "Permissions", description: "Application permission catalog", icon: KeyRound, permission: "PERMISSION_READ" as const },
  ].filter((link) => hasPermission(link.permission));

  return <section className="mt-6"><div className="mb-4 flex items-end justify-between"><div><p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">User management</p><h2 className="mt-1 text-lg font-semibold text-slate-900">Customers and service access</h2></div><Link href="/customers" className="text-sm font-semibold text-primary hover:underline">Open customers</Link></div><Card><CardHeader><div className="flex items-center gap-3"><div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600"><CheckCircle2 className="h-5 w-5" /></div><div><CardTitle>Admin customer API connected</CardTitle><p className="mt-1 text-sm text-muted-foreground">Customer Service now exposes CUSTOMER_READ-protected profile and shipping/billing address operations.</p></div></div></CardHeader><CardContent><div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">{links.map(({ href, label, description, icon: Icon }) => <Link href={href} key={href} className="group rounded-2xl border border-slate-100 p-4 transition-colors hover:border-blue-100 hover:bg-blue-50/50"><div className="flex items-center gap-3"><div className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-slate-500 group-hover:bg-blue-100 group-hover:text-primary"><Icon className="h-4 w-4" /></div><ArrowUpRight className="ml-auto h-4 w-4 text-slate-300 group-hover:text-primary" /></div><p className="mt-3 text-sm font-semibold text-slate-800">{label}</p><p className="mt-1 text-xs leading-5 text-slate-500">{description}</p></Link>)}</div></CardContent></Card></section>;
}
