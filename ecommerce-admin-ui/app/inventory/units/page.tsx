"use client";

import Link from "next/link";
import { Search } from "lucide-react";
import { useEffect, useState } from "react";
import { PageIntro } from "@/components/page-intro";
import { InventoryPagination, InventoryServiceUnavailable, SkuLink } from "@/components/inventory/inventory-common";
import { UnitStatusBadge } from "@/components/inventory/status-badges";
import { EmptyState, LoadingCard } from "@/components/feedback-states";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { useInventoryLocations, useInventoryUnits } from "@/lib/queries";
import type { InventoryUnitStatus } from "@/lib/types";

const statuses: InventoryUnitStatus[] = ["AVAILABLE", "RESERVED", "ALLOCATED", "IN_TRANSIT", "SOLD", "RETURNED", "DAMAGED", "LOST"];

export default function UnitsPage() {
  const [page, setPage] = useState(0);
  const [sku, setSku] = useState("");
  const [locationId, setLocationId] = useState("");
  const [status, setStatus] = useState<InventoryUnitStatus | "">("");
  const [serialNumber, setSerialNumber] = useState("");
  const [imei, setImei] = useState("");
  const [barcode, setBarcode] = useState("");
  const locations = useInventoryLocations();
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    setSku(params.get("sku") ?? "");
    setLocationId(params.get("locationId") ?? "");
  }, []);
  const units = useInventoryUnits({ page, size: 20, sort: "createdAt,desc", sku: sku.trim().toUpperCase() || undefined, locationId: locationId || undefined, status: status || undefined, serialNumber: serialNumber.trim() || undefined, imei: imei.trim() || undefined, barcode: barcode.trim() || undefined });
  const clear = () => { setSku(""); setLocationId(""); setStatus(""); setSerialNumber(""); setImei(""); setBarcode(""); setPage(0); };
  return <>
    <PageIntro eyebrow="Inventory operations" title="Inventory units" description="Every row is one physical item. SKU identifies what is sold; Inventory Unit ID identifies the item itself." />
    <Card><CardContent className="p-4"><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <div className="relative"><Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><Input className="pl-9 uppercase" placeholder="SKU" value={sku} onChange={(event) => { setSku(event.target.value); setPage(0); }} /></div>
      <Input placeholder="Serial number" value={serialNumber} onChange={(event) => { setSerialNumber(event.target.value); setPage(0); }} />
      <Input placeholder="IMEI" value={imei} onChange={(event) => { setImei(event.target.value); setPage(0); }} />
      <Input placeholder="Barcode" value={barcode} onChange={(event) => { setBarcode(event.target.value); setPage(0); }} />
      <Select value={locationId} onChange={(event) => { setLocationId(event.target.value); setPage(0); }}><option value="">All locations</option>{(locations.data ?? []).map((location) => <option value={location.id} key={location.id}>{location.code}</option>)}</Select>
      <Select value={status} onChange={(event) => { setStatus(event.target.value as InventoryUnitStatus | ""); setPage(0); }}><option value="">All statuses</option>{statuses.map((item) => <option value={item} key={item}>{item.replaceAll("_", " ")}</option>)}</Select>
      <button className="text-left text-sm font-semibold text-primary hover:underline sm:col-span-2 lg:col-span-2" onClick={clear}>Clear all filters</button>
    </div></CardContent></Card>
    <div className="mt-5">{!sku.trim() ? <EmptyState title="Select a SKU first" message="The Inventory API lists physical units by SKU. Open this page from Stock or enter a SKU above." /> : units.isLoading ? <LoadingCard rows={8} /> : units.isError ? <InventoryServiceUnavailable error={units.error} onRetry={() => void units.refetch()} /> : units.data?.content.length === 0 ? <EmptyState title="No inventory units found" message="Adjust the filters or receive inventory to create physical units." /> : <Card className="overflow-hidden"><div className="overflow-x-auto"><table className="w-full min-w-[1100px] text-left"><thead><tr className="border-b border-slate-100 text-xs font-semibold uppercase tracking-[0.1em] text-slate-400"><th className="px-5 py-4">Inventory Unit ID</th><th className="px-3 py-4">Unit code</th><th className="px-3 py-4">SKU</th><th className="px-3 py-4">Product</th><th className="px-3 py-4">Location</th><th className="px-3 py-4">Status</th><th className="px-3 py-4">Serial</th><th className="px-3 py-4">IMEI</th><th className="px-3 py-4">Barcode</th></tr></thead><tbody>{units.data?.content.map((unit) => <tr className="table-row" key={unit.id}><td className="px-5 py-4"><Link href={`/inventory/units/${unit.id}`} className="font-mono text-xs font-semibold text-primary hover:underline">{unit.id}</Link></td><td className="px-3 py-4 font-mono text-sm text-slate-700">{unit.unitCode}</td><td className="px-3 py-4"><SkuLink sku={unit.sku} /></td><td className="px-3 py-4 text-sm text-slate-500">{unit.productName ?? "Catalog lookup unavailable"}</td><td className="px-3 py-4 text-sm text-slate-600">{unit.location.code}</td><td className="px-3 py-4"><UnitStatusBadge status={unit.status} /></td><td className="px-3 py-4 text-sm text-slate-600">{unit.serialNumber ?? "—"}</td><td className="px-3 py-4 text-sm text-slate-600">{unit.imei ?? "—"}</td><td className="px-3 py-4 text-sm text-slate-600">{unit.barcode ?? "—"}</td></tr>)}</tbody></table></div>{units.data && <InventoryPagination page={units.data} onPageChange={setPage} />}</Card>}</div>
  </>;
}
