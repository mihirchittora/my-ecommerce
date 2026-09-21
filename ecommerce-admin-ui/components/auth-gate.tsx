"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { AppShell } from "@/components/app-shell";
import { useAuth } from "@/components/auth-provider";
import { defaultRouteForUser, hasAllPermissions, hasAnyPermission, isInternalUser, routeAccess } from "@/lib/permissions";

function LoadingScreen() {
  return <main className="flex min-h-screen items-center justify-center bg-background p-6"><div className="rounded-2xl border border-slate-200 bg-white px-6 py-5 text-sm text-slate-500 shadow-sm">Checking your workspace access…</div></main>;
}

function AccessDenied({ message = "You do not have permission to open this workspace." }: { message?: string }) {
  const { logout } = useAuth();
  const router = useRouter();
  return <main className="flex min-h-screen items-center justify-center bg-background p-6"><div className="max-w-md rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm"><p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">Access restricted</p><h1 className="mt-3 text-2xl font-semibold text-slate-950">Admin access required</h1><p className="mt-3 text-sm leading-6 text-slate-500">{message}</p><button type="button" className="mt-6 text-sm font-semibold text-primary hover:underline" onClick={() => void logout().then(() => router.replace("/login"))}>Sign out</button></div></main>;
}

export function AuthGate({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, status } = useAuth();

  useEffect(() => {
    if (pathname === "/login" && status === "authenticated") router.replace(defaultRouteForUser(user));
    if (pathname !== "/login" && status === "unauthenticated") router.replace(`/login?next=${encodeURIComponent(pathname)}`);
  }, [pathname, router, status, user]);

  if (pathname === "/login") return <>{children}</>;
  if (status === "loading" || status === "unauthenticated") return <LoadingScreen />;
  if (!isInternalUser(user)) return <AccessDenied message="This application is exclusively for internal catalog and operations users." />;

  const access = routeAccess(pathname);
  if (access.allOf && !hasAllPermissions(user, access.allOf)) return <AccessDenied />;
  if (access.anyOf && !hasAnyPermission(user, access.anyOf)) return <AccessDenied />;
  return <AppShell>{children}</AppShell>;
}
