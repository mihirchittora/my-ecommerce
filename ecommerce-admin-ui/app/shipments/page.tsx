"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { PageIntro } from "@/components/page-intro";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { ShipmentTable } from "@/components/shipping/shipment-table";
import { ShippingSortControl } from "@/components/shipping/sort-control";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { ApiError } from "@/lib/api/client";
import { useShipments } from "@/lib/api/shipping/queries";
import { isShipmentStatus, SHIPMENT_STATUSES, type ShipmentListParams, type ShipmentSort, type ShipmentStatus } from "@/lib/api/shipping/types";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

const DEFAULT_SORT: ShipmentSort = "createdAt,desc";
const SORTS: ShipmentSort[] = ["createdAt,desc", "createdAt,asc", "updatedAt,desc", "updatedAt,asc", "status,asc", "status,desc", "shipmentNumber,asc", "shipmentNumber,desc", "orderNumber,asc", "orderNumber,desc"];

type ShipmentFilterDraft = { search: string; status: ShipmentStatus | ""; carrier: string; createdFrom: string; createdTo: string };

function pageValue(value: string | null) { const parsed = Number(value); return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0; }
function sizeValue(value: string | null) { return value === "50" || value === "100" ? Number(value) : 20; }
function sortValue(value: string | null): ShipmentSort { return value && SORTS.includes(value as ShipmentSort) ? value as ShipmentSort : DEFAULT_SORT; }
function statusValue(value: string | null): ShipmentStatus | undefined { return value && isShipmentStatus(value) ? value : undefined; }
function errorMessage(error: unknown) { if (error instanceof ApiError) { if (error.status === 403) return "You do not have permission to view shipments."; if (error.status === 0 || error.status >= 500) return "Shipping Service is temporarily unavailable."; return error.message; } return "Shipping Service is temporarily unavailable."; }

export default function ShipmentsPage() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const filters = useMemo<ShipmentListParams>(() => ({
    page: pageValue(searchParams.get("page")),
    size: sizeValue(searchParams.get("size")),
    sort: sortValue(searchParams.get("sort")),
    search: searchParams.get("search") ?? undefined,
    status: statusValue(searchParams.get("status")),
    carrier: searchParams.get("carrier") ?? undefined,
    createdFrom: searchParams.get("createdFrom") ?? undefined,
    createdTo: searchParams.get("createdTo") ?? undefined,
  }), [searchParams]);
  const [draft, setDraft] = useState<ShipmentFilterDraft>({ search: filters.search ?? "", status: filters.status ?? "", carrier: filters.carrier ?? "", createdFrom: filters.createdFrom ?? "", createdTo: filters.createdTo ?? "" });
  const shipments = useShipments(filters);

  useEffect(() => {
    setDraft({ search: filters.search ?? "", status: filters.status ?? "", carrier: filters.carrier ?? "", createdFrom: filters.createdFrom ?? "", createdTo: filters.createdTo ?? "" });
  }, [filters.search, filters.status, filters.carrier, filters.createdFrom, filters.createdTo]);

  const updateParams = useCallback((changes: Record<string, string | number | undefined>) => {
    const next = new URLSearchParams(searchParams.toString());
    Object.entries(changes).forEach(([key, value]) => { if (value === undefined || value === "") next.delete(key); else next.set(key, String(value)); });
    if (!Object.hasOwn(changes, "page")) next.set("page", "0");
    const query = next.toString();
    router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
  }, [pathname, router, searchParams]);

  const applyFilters = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    updateParams({ search: draft.search.trim() || undefined, status: draft.status || undefined, carrier: draft.carrier.trim() || undefined, createdFrom: draft.createdFrom || undefined, createdTo: draft.createdTo || undefined });
  };

  const clearFilters = () => {
    setDraft({ search: "", status: "", carrier: "", createdFrom: "", createdTo: "" });
    updateParams({ search: undefined, status: undefined, carrier: undefined, createdFrom: undefined, createdTo: undefined });
  };

  return <><PageIntro eyebrow="Shipping operations" title="Shipments" description="Monitor carrier-facing shipments, physical unit references, tracking, and safe lifecycle actions." /><form onSubmit={applyFilters} className="mb-5 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5"><label className="text-sm font-medium text-slate-700 xl:col-span-2">Search shipment, order, or tracking<Input className="mt-1.5" placeholder="e.g. SHP-2026…" value={draft.search} onChange={(event) => setDraft((current) => ({ ...current, search: event.target.value }))} /></label><label className="text-sm font-medium text-slate-700">Status<Select className="mt-1.5" value={draft.status} onChange={(event) => setDraft((current) => ({ ...current, status: event.target.value as ShipmentStatus | "" }))}><option value="">All statuses</option>{SHIPMENT_STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}</Select></label><label className="text-sm font-medium text-slate-700">Carrier<Input className="mt-1.5" placeholder="e.g. SANDBOX" value={draft.carrier} onChange={(event) => setDraft((current) => ({ ...current, carrier: event.target.value }))} /></label><div className="grid grid-cols-2 gap-3 md:col-span-2 xl:col-span-5 xl:grid-cols-4"><label className="text-sm font-medium text-slate-700">Created from<Input className="mt-1.5" type="date" value={draft.createdFrom} onChange={(event) => setDraft((current) => ({ ...current, createdFrom: event.target.value }))} /></label><label className="text-sm font-medium text-slate-700">Created to<Input className="mt-1.5" type="date" value={draft.createdTo} onChange={(event) => setDraft((current) => ({ ...current, createdTo: event.target.value }))} /></label><div className="col-span-2 flex items-end justify-end gap-2"><Button type="button" variant="outline" onClick={clearFilters}>Clear</Button><Button type="submit">Apply filters</Button></div></div></div></form><div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><p className="text-sm text-slate-500">{shipments.data ? `${shipments.data.totalElements} shipment${shipments.data.totalElements === 1 ? "" : "s"}` : "Loading shipments…"}</p><ShippingSortControl kind="shipment" value={filters.sort} onChange={(sort) => updateParams({ sort })} /></div>{shipments.isLoading ? <LoadingCard rows={7} /> : shipments.isError ? <ErrorState title="Shipments are unavailable" message={errorMessage(shipments.error)} onRetry={() => void shipments.refetch()} /> : shipments.data?.content.length === 0 ? <EmptyState title="No shipments found" message="Try changing the shipment filters or return to the full list." action={filters.page > 0 ? <Link href="/shipments" className="text-sm font-semibold text-primary hover:underline">Return to first page</Link> : undefined} /> : shipments.data && <ShipmentTable page={shipments.data} pageSize={filters.size} onPageChange={(page) => updateParams({ page })} onPageSizeChange={(size) => updateParams({ size, page: 0 })} />}</>;
}
