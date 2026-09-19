"use client";

import Link from "next/link";
import { ArrowRight, Boxes, Layers3, Package, Plus, Shapes, Sparkles } from "lucide-react";
import { PageIntro } from "@/components/page-intro";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useCategoryTree } from "@/lib/queries";
import { useProductList } from "@/lib/queries";
import { ErrorState } from "@/components/feedback-states";

function countCategories(nodes: Array<{ children: unknown[] }>): number { return nodes.reduce((count, node) => count + 1 + countCategories(node.children as Array<{ children: unknown[] }>), 0); }

export default function DashboardPage() {
  const categories = useCategoryTree();
  const products = useProductList({ page: 0, size: 1, sort: "createdAt,desc" });
  const categoryCount = categories.data ? countCategories(categories.data) : 0;
  return <div className="page-grid -m-4 min-h-[calc(100vh-5rem)] p-4 sm:-m-6 sm:p-6 lg:-m-10 lg:p-10"><PageIntro eyebrow="Overview" title="Good morning, catalog team" description="A focused view of the catalog so you can keep your assortment clean, complete and ready to publish." action={{ label: "Add product", href: "/products/new" }} /><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><MetricCard label="Products" value={products.data?.totalElements ?? "—"} icon={Package} tone="blue" href="/products" /><MetricCard label="Categories" value={categories.isLoading ? "—" : categoryCount} icon={Shapes} tone="violet" href="/categories" /><MetricCard label="Variants" value="—" icon={Layers3} tone="amber" href="/products" /><MetricCard label="Catalog status" value="Healthy" icon={Sparkles} tone="green" /></div><div className="mt-6 grid gap-6 xl:grid-cols-[1.4fr_0.8fr]"><Card><CardHeader className="flex-row items-center justify-between"><div><CardTitle>Quick actions</CardTitle><p className="mt-1 text-sm text-muted-foreground">Common catalog operations, one click away.</p></div><Boxes className="h-5 w-5 text-slate-300" /></CardHeader><CardContent className="grid gap-3 sm:grid-cols-2"><QuickAction href="/products/new" icon={Plus} title="Create a product" description="Add product details, variants and pricing." /><QuickAction href="/categories" icon={Shapes} title="Organize categories" description="Build a clear hierarchy for browsing." /><QuickAction href="/products" icon={Package} title="Review assortment" description="Search, filter and manage products." /><QuickAction href="/products" icon={Layers3} title="Check variants" description="Keep SKUs and availability accurate." /></CardContent></Card><Card className="overflow-hidden bg-slate-950 text-white"><CardHeader><div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-white/10"><Sparkles className="h-5 w-5 text-blue-300" /></div><CardTitle className="text-white">Catalog health</CardTitle><p className="mt-1 text-sm leading-6 text-slate-400">Use the product workspace to keep every sellable variant discoverable and correctly priced.</p></CardHeader><CardContent><Link href="/products" className="inline-flex items-center gap-2 text-sm font-semibold text-blue-300 hover:text-blue-200">Open products <ArrowRight className="h-4 w-4" /></Link></CardContent></Card></div>{(categories.isError || products.isError) && <div className="mt-6"><ErrorState message="The catalog service is unavailable. You can still browse the workspace navigation." /></div>}</div>;
}

function MetricCard({ label, value, icon: Icon, tone, href }: { label: string; value: string | number; icon: typeof Package; tone: "blue" | "violet" | "amber" | "green"; href?: string }) {
  const styles = { blue: "bg-blue-50 text-blue-600", violet: "bg-violet-50 text-violet-600", amber: "bg-amber-50 text-amber-600", green: "bg-emerald-50 text-emerald-600" };
  const body = <Card className="p-5"><div className="flex items-start justify-between"><div><p className="text-sm font-medium text-slate-500">{label}</p><p className="mt-3 text-2xl font-bold tracking-tight text-slate-950">{value}</p></div><div className={`flex h-10 w-10 items-center justify-center rounded-xl ${styles[tone]}`}><Icon className="h-5 w-5" /></div></div></Card>;
  return href ? <Link href={href} className="block transition-transform hover:-translate-y-0.5">{body}</Link> : body;
}

function QuickAction({ href, icon: Icon, title, description }: { href: string; icon: typeof Plus; title: string; description: string }) { return <Link href={href} className="group rounded-2xl border border-slate-100 p-4 transition-colors hover:border-blue-100 hover:bg-blue-50/50"><div className="flex items-center gap-3"><div className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-slate-500 group-hover:bg-blue-100 group-hover:text-primary"><Icon className="h-4 w-4" /></div><div className="min-w-0"><p className="text-sm font-semibold text-slate-800">{title}</p><p className="mt-1 text-xs leading-5 text-slate-500">{description}</p></div><ArrowRight className="ml-auto h-4 w-4 text-slate-300 group-hover:text-primary" /></div></Link>; }
