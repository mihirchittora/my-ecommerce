import { ArrowUpRight } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";

export function PageIntro({ eyebrow, title, description, action }: { eyebrow?: string; title: string; description: string; action?: { label: string; href: string } }) {
  return <div className="mb-8 flex flex-col gap-5 md:flex-row md:items-end md:justify-between"><div><p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">{eyebrow ?? "Catalog workspace"}</p><h1 className="text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">{title}</h1><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">{description}</p></div>{action && <Button asChild><Link href={action.href}>{action.label}<ArrowUpRight className="h-4 w-4" /></Link></Button>}</div>;
}
