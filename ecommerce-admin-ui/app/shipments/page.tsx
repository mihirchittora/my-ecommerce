"use client";

import Link from "next/link";
import { useCallback, useMemo } from "react";
import { PageIntro } from "@/components/page-intro";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { ShipmentTable } from "@/components/shipping/shipment-table";
import { ShippingSortControl } from "@/components/shipping/sort-control";
import { ApiError } from "@/lib/api/client";
import { useShipments } from "@/lib/api/shipping/queries";
import type { ShipmentListParams, ShipmentSort } from "@/lib/api/shipping/types";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

const DEFAULT_SORT: ShipmentSort = "createdAt,desc";
const SORTS: ShipmentSort[] = ["createdAt,desc", "createdAt,asc", "updatedAt,desc", "updatedAt,asc", "status,asc", "status,desc", "shipmentNumber,asc", "shipmentNumber,desc", "orderNumber,asc", "orderNumber,desc"];

function pageValue(value: string | null) { const parsed = Number(value); return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0; }
function sizeValue(value: string | null) { return value === "50" || value === "100" ? Number(value) : 20; }
function sortValue(value: string | null): ShipmentSort { return value && SORTS.includes(value as ShipmentSort) ? value as ShipmentSort : DEFAULT_SORT; }
function errorMessage(error: unknown) { if (error instanceof ApiError) { if (error.status === 403) return "You do not have permission to view shipments."; if (error.status === 0 || error.status >= 500) return "Shipping Service is temporarily unavailable."; return error.message; } return "Shipping Service is temporarily unavailable."; }

export default function ShipmentsPage() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const filters = useMemo<ShipmentListParams>(() => ({ page: pageValue(searchParams.get("page")), size: sizeValue(searchParams.get("size")), sort: sortValue(searchParams.get("sort")) }), [searchParams]);
  const shipments = useShipments(filters);
  const updateParams = useCallback((changes: Record<string, string | number | undefined>) => { const next = new URLSearchParams(searchParams.toString()); Object.entries(changes).forEach(([key, value]) => { if (value === undefined || value === "") next.delete(key); else next.set(key, String(value)); }); if (!Object.hasOwn(changes, "page")) next.set("page", "0"); const query = next.toString(); router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false }); }, [pathname, router, searchParams]);
  return <><PageIntro eyebrow="Shipping operations" title="Shipments" description="Monitor carrier-facing shipments, physical unit references, tracking, and safe lifecycle actions." /><div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><p className="text-sm text-slate-500">{shipments.data ? `${shipments.data.totalElements} shipment${shipments.data.totalElements === 1 ? "" : "s"}` : "Loading shipments…"}</p><ShippingSortControl kind="shipment" value={filters.sort} onChange={(sort) => updateParams({ sort })} /></div><div className="mb-4 rounded-2xl border border-blue-100 bg-blue-50/70 px-4 py-3 text-sm leading-6 text-blue-900">The current Shipping API supports server pagination and sorting. Search and status/carrier/date filters are not exposed by the service yet, so this screen does not filter in the browser.</div>{shipments.isLoading ? <LoadingCard rows={7} /> : shipments.isError ? <ErrorState title="Shipments are unavailable" message={errorMessage(shipments.error)} onRetry={() => void shipments.refetch()} /> : shipments.data?.content.length === 0 ? <EmptyState title="No shipments found" message="Shipping Service has not returned any shipments for this page." action={filters.page > 0 ? <Link href="/shipments" className="text-sm font-semibold text-primary hover:underline">Return to first page</Link> : undefined} /> : shipments.data && <ShipmentTable page={shipments.data} pageSize={filters.size} onPageChange={(page) => updateParams({ page })} onPageSizeChange={(size) => updateParams({ size, page: 0 })} />}</>;
}
