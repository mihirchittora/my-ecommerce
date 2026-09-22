"use client";

import Link from "next/link";
import { useCallback, useMemo } from "react";
import { PageIntro } from "@/components/page-intro";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { FulfillmentTable } from "@/components/shipping/fulfillment-table";
import { ShippingSortControl } from "@/components/shipping/sort-control";
import { ApiError } from "@/lib/api/client";
import { useFulfillments } from "@/lib/api/shipping/queries";
import type { FulfillmentListParams, FulfillmentSort } from "@/lib/api/shipping/types";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

const DEFAULT_SORT: FulfillmentSort = "createdAt,desc";
const SORTS: FulfillmentSort[] = ["createdAt,desc", "createdAt,asc", "updatedAt,desc", "updatedAt,asc", "status,asc", "status,desc", "orderNumber,asc", "orderNumber,desc"];
function pageValue(value: string | null) { const parsed = Number(value); return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0; }
function sizeValue(value: string | null) { return value === "50" || value === "100" ? Number(value) : 20; }
function sortValue(value: string | null): FulfillmentSort { return value && SORTS.includes(value as FulfillmentSort) ? value as FulfillmentSort : DEFAULT_SORT; }
function errorMessage(error: unknown) { if (error instanceof ApiError) { if (error.status === 403) return "You do not have permission to view fulfillments."; if (error.status === 0 || error.status >= 500) return "Shipping Service is temporarily unavailable."; return error.message; } return "Shipping Service is temporarily unavailable."; }

export default function FulfillmentsPage() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const filters = useMemo<FulfillmentListParams>(() => ({ page: pageValue(searchParams.get("page")), size: sizeValue(searchParams.get("size")), sort: sortValue(searchParams.get("sort")) }), [searchParams]);
  const fulfillments = useFulfillments(filters);
  const updateParams = useCallback((changes: Record<string, string | number | undefined>) => { const next = new URLSearchParams(searchParams.toString()); Object.entries(changes).forEach(([key, value]) => { if (value === undefined || value === "") next.delete(key); else next.set(key, String(value)); }); if (!Object.hasOwn(changes, "page")) next.set("page", "0"); const query = next.toString(); router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false }); }, [pathname, router, searchParams]);
  return <><PageIntro eyebrow="Shipping operations" title="Fulfillments" description="Follow operational work from an Order snapshot through itemized shipments and physical unit references." /><div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><p className="text-sm text-slate-500">{fulfillments.data ? `${fulfillments.data.totalElements} fulfillment${fulfillments.data.totalElements === 1 ? "" : "s"}` : "Loading fulfillments…"}</p><ShippingSortControl kind="fulfillment" value={filters.sort} onChange={(sort) => updateParams({ sort })} /></div><div className="mb-4 rounded-2xl border border-blue-100 bg-blue-50/70 px-4 py-3 text-sm leading-6 text-blue-900">The current Fulfillment API supports server pagination and sorting only. Status, order, customer, and date filters are not currently supported by the backend.</div>{fulfillments.isLoading ? <LoadingCard rows={7} /> : fulfillments.isError ? <ErrorState title="Fulfillments are unavailable" message={errorMessage(fulfillments.error)} onRetry={() => void fulfillments.refetch()} /> : fulfillments.data?.content.length === 0 ? <EmptyState title="No fulfillments found" message="Shipping Service has not returned any fulfillments for this page." action={filters.page > 0 ? <Link href="/fulfillments" className="text-sm font-semibold text-primary hover:underline">Return to first page</Link> : undefined} /> : fulfillments.data && <FulfillmentTable page={fulfillments.data} pageSize={filters.size} onPageChange={(page) => updateParams({ page })} onPageSizeChange={(size) => updateParams({ size, page: 0 })} />}</>;
}
