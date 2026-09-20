"use client";

import Link from "next/link";
import { ArrowLeft, Cuboid } from "lucide-react";
import { useParams, useSearchParams } from "next/navigation";
import { PageIntro } from "@/components/page-intro";
import { InventoryServiceUnavailable } from "@/components/inventory/inventory-common";
import { UnitStatusBadge } from "@/components/inventory/status-badges";
import { EmptyState, LoadingCard } from "@/components/feedback-states";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useInventoryStock, useInventoryUnits } from "@/lib/queries";

export default function StockDetailPage() {
  const params = useParams<{ sku: string }>();
  const sku = decodeURIComponent(params.sku);
  const locationId = useSearchParams().get("locationId") ?? undefined;
  const stock = useInventoryStock({ page: 0, size: 100, sort: "sku,asc", sku, locationId });
  const units = useInventoryUnits({ page: 0, size: 10, sort: "createdAt,asc", sku, locationId });
  return <>
    <div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/inventory/stock"><ArrowLeft className="h-4 w-4" />Back to stock</Link></Button></div>
    <PageIntro eyebrow="Stock detail" title={sku} description="Aggregated stock by location with a direct path to the physical inventory units." />
    {stock.isLoading ? <LoadingCard rows={4} /> : stock.isError ? <InventoryServiceUnavailable error={stock.error} onRetry={() => void stock.refetch()} /> : <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]"><Card><CardHeader><div className="flex items-center gap-2"><Cuboid className="h-5 w-5 text-primary" /><div><CardTitle>Location balances</CardTitle><p className="mt-1 text-sm text-muted-foreground">The same SKU may exist at multiple locations.</p></div></div></CardHeader><CardContent className="p-0"><div className="overflow-x-auto"><table className="w-full min-w-[560px] text-left"><thead><tr className="border-y border-slate-100 text-xs font-semibold uppercase tracking-[0.1em] text-slate-400"><th className="px-5 py-3">Location</th><th className="px-3 py-3">Total</th><th className="px-3 py-3">Available</th><th className="px-3 py-3">Reserved</th><th className="px-3 py-3">Units</th></tr></thead><tbody>{stock.data?.content.map((row) => <tr className="table-row" key={`${row.sku}-${row.location?.id ?? row.locationCount}`}><td className="px-5 py-4 text-sm font-semibold text-slate-700">{row.location?.name ?? row.location?.code ?? "Location unavailable"}</td><td className="px-3 py-4 text-sm">{row.totalUnits}</td><td className="px-3 py-4 text-sm text-emerald-700">{row.available}</td><td className="px-3 py-4 text-sm text-amber-700">{row.reserved}</td><td className="px-3 py-4"><Link href={`/inventory/units?sku=${encodeURIComponent(row.sku)}${row.location?.id ? `&locationId=${row.location.id}` : ""}`} className="text-sm font-semibold text-primary hover:underline">View units</Link></td></tr>)}</tbody></table></div></CardContent></Card><Card><CardHeader><CardTitle>Physical units</CardTitle><p className="mt-1 text-sm text-muted-foreground">Itemized inventory for {sku}.</p></CardHeader><CardContent>{units.isLoading ? <div className="space-y-3">{Array.from({ length: 4 }).map((_, index) => <div className="h-10 animate-pulse rounded-xl bg-slate-100" key={index} />)}</div> : units.isError ? <p className="text-sm text-amber-700">Units are temporarily unavailable.</p> : units.data?.content.length === 0 ? <EmptyState title="No physical units" message="This SKU has no itemized units at this location." /> : <div className="space-y-2">{units.data?.content.slice(0, 5).map((unit) => <Link href={`/inventory/units/${unit.id}`} className="flex items-center justify-between rounded-xl border border-slate-100 p-3 hover:border-blue-100 hover:bg-blue-50/40" key={unit.id}><span><span className="block font-mono text-sm font-semibold text-slate-700">{unit.unitCode}</span><span className="mt-1 block text-xs text-slate-400">{unit.serialNumber ?? "No serial / IMEI"}</span></span><UnitStatusBadge status={unit.status} /></Link>)}</div>}</CardContent></Card></div>}
  </>;
}
