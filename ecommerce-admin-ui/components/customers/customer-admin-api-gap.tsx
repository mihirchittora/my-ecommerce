import Link from "next/link";
import { AlertTriangle, ArrowUpRight } from "lucide-react";
import { useAuth } from "@/components/auth-provider";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function CustomerAdminApiGap({ detailId }: { detailId?: string }) {
  const { hasPermission } = useAuth();
  return <Card className="overflow-hidden border-amber-200 bg-amber-50/40"><CardHeader><div className="flex items-start gap-3"><div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-amber-100 text-amber-700"><AlertTriangle className="h-5 w-5" /></div><div><CardTitle>{detailId ? "Customer detail moved" : "Customer administration moved"}</CardTitle><p className="mt-1 max-w-2xl text-sm leading-6 text-slate-600">The live customer administration screens now load profile and address data from the CUSTOMER_READ-protected Customer Service contract.</p></div></div></CardHeader><CardContent><div className="flex flex-wrap gap-4">{hasPermission("PERMISSION_READ") && <Link href="/permissions" className="inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline">Review permission catalog <ArrowUpRight className="h-4 w-4" /></Link>}<Link href="/customers" className="inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline">Open customers <ArrowUpRight className="h-4 w-4" /></Link></div></CardContent></Card>;
}
