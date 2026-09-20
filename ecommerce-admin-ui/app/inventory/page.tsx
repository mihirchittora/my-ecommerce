"use client";

import Link from "next/link";
import { useState } from "react";
import { ArrowUpRight, PackageSearch } from "lucide-react";
import { PageIntro } from "@/components/page-intro";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { InventoryServiceUnavailable, InventoryPagination, SkuLink } from "@/components/inventory/inventory-common";
import { EmptyState, LoadingCard } from "@/components/feedback-states";
import { useInventoryStock } from "@/lib/queries";

export default function InventoryOverviewPage() {
  const [page, setPage] = useState(0);
  const stock = useInventoryStock({ page, size: 10, sort: "sku,asc" });
  return <>
    <PageIntro eyebrow="Inventory operations" title="Inventory overview" description="See stock at SKU level, then open itemized units when physical inventory matters." action={{ label: "Receive inventory", href: "/inventory/receive" }} />
    <Card><CardContent className="p-5"><p className="text-sm font-semibold text-slate-800">SKU-scoped inventory</p><p className="mt-1 text-sm leading-6 text-slate-500">The current Inventory API reports stock for a requested SKU. Open a product detail or use Stock to select a SKU; global totals are not invented here.</p></CardContent></Card>
    <div className="mt-6">{stock.isLoading ? <LoadingCard rows={6} /> : stock.isError ? <InventoryServiceUnavailable error={stock.error} onRetry={() => void stock.refetch()} /> : stock.data?.content.length === 0 ? <EmptyState title="No inventory stock yet" message="Receive inventory to create SKU-level stock and physical units." action={<Link href="/inventory/receive" className="inline-flex items-center gap-2 text-sm font-semibold text-primary hover:underline">Receive stock <ArrowUpRight className="h-4 w-4" /></Link>} /> : <Card className="overflow-hidden"><CardHeader className="flex-row items-center justify-between"><div><CardTitle>Stock by SKU</CardTitle><p className="mt-1 text-sm text-muted-foreground">SKU is the sellable configuration; unit IDs represent physical items.</p></div><PackageSearch className="h-5 w-5 text-slate-300" /></CardHeader><CardContent className="p-0"><div className="overflow-x-auto"><table className="w-full min-w-[760px] text-left"><thead><tr className="border-y border-slate-100 text-xs font-semibold uppercase tracking-[0.1em] text-slate-400"><th className="px-5 py-3">SKU</th><th className="px-3 py-3">Product</th><th className="px-3 py-3">Locations</th><th className="px-3 py-3">Total units</th><th className="px-3 py-3">Available</th><th className="px-3 py-3">Reserved</th><th className="px-3 py-3">Damaged</th><th className="px-3 py-3">Units</th></tr></thead><tbody>{stock.data?.content.map((row) => <tr className="table-row" key={row.sku}><td className="px-5 py-4"><SkuLink sku={row.sku} /></td><td className="px-3 py-4 text-sm text-slate-600">{row.productName ?? "Catalog lookup unavailable"}</td><td className="px-3 py-4 text-sm text-slate-600">{row.locationCount}</td><td className="px-3 py-4 text-sm font-semibold text-slate-700">{row.totalUnits}</td><td className="px-3 py-4 text-sm text-emerald-700">{row.available}</td><td className="px-3 py-4 text-sm text-amber-700">{row.reserved}</td><td className="px-3 py-4 text-sm text-rose-700">{row.damaged}</td><td className="px-3 py-4"><Link href={`/inventory/units?sku=${encodeURIComponent(row.sku)}`} className="text-sm font-semibold text-primary hover:underline">View units</Link></td></tr>)}</tbody></table></div>{stock.data && <InventoryPagination page={stock.data} onPageChange={setPage} />}</CardContent></Card>}</div>
  </>;
}
