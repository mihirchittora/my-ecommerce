"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/components/auth-provider";
import { Spinner } from "@/components/ui/spinner";

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { status } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status === "unauthenticated") router.replace(`/login?next=${encodeURIComponent(pathname)}`);
  }, [pathname, router, status]);

  if (status !== "authenticated") return <div className="flex min-h-[45vh] items-center justify-center"><Spinner label="Loading your account" /></div>;
  return <>{children}</>;
}
