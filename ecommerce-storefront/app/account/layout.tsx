import { RequireAuth } from "@/components/require-auth";
import { AccountNav } from "@/components/account-nav";

export default function AccountLayout({ children }: { children: React.ReactNode }) { return <RequireAuth><div className="page-shell py-10 md:py-14"><div className="mb-8"><p className="eyebrow">Your space</p><h1 className="mt-3 font-display text-4xl font-bold tracking-tight">Account</h1></div><AccountNav /><div className="mt-8">{children}</div></div></RequireAuth>; }
