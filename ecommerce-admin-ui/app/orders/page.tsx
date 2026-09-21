"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { PageIntro } from "@/components/page-intro";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { OrderFilters, OrderSortControl, isOrderStatus } from "@/components/orders/order-filters";
import { OrderTable } from "@/components/orders/order-table";
import { ApiError } from "@/lib/api/client";
import { ORDER_STATUSES, type OrderListParams, type OrderSort } from "@/lib/api/order/types";
import { useOrderList } from "@/lib/api/order/queries";

const DEFAULT_SIZE = 20;
const DEFAULT_SORT: OrderSort = "createdAt,desc";
const SORTS: OrderSort[] = ["createdAt,desc", "createdAt,asc", "updatedAt,desc", "updatedAt,asc", "orderNumber,asc", "orderNumber,desc", "totalAmount,desc", "totalAmount,asc", "status,asc", "status,desc"];

function readPage(value: string | null) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
}

function readSize(value: string | null) {
  return value === "50" || value === "100" ? Number(value) : DEFAULT_SIZE;
}

function readSort(value: string | null): OrderSort {
  return value && SORTS.includes(value as OrderSort) ? value as OrderSort : DEFAULT_SORT;
}

function readFilters(searchParams: ReturnType<typeof useSearchParams>): OrderListParams {
  const status = searchParams.get("status");
  return {
    page: readPage(searchParams.get("page")),
    size: readSize(searchParams.get("size")),
    sort: readSort(searchParams.get("sort")),
    orderNumber: searchParams.get("search") || undefined,
    status: status && isOrderStatus(status) ? status : undefined,
    sku: searchParams.get("sku") || undefined,
    createdFrom: searchParams.get("createdFrom") || undefined,
    createdTo: searchParams.get("createdTo") || undefined,
  };
}

function errorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 403) return "You do not have permission to view orders.";
    if (error.status === 503 || error.status === 0) return "Order service is temporarily unavailable.";
    return error.message;
  }
  return "Order service is temporarily unavailable.";
}

export default function OrdersPage() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const filters = useMemo(() => readFilters(searchParams), [searchParams]);
  const [searchValue, setSearchValue] = useState(filters.orderNumber ?? "");
  const orders = useOrderList(filters);

  useEffect(() => setSearchValue(filters.orderNumber ?? ""), [filters.orderNumber]);

  const updateParams = useCallback((changes: Record<string, string | number | undefined>) => {
    const next = new URLSearchParams(searchParams.toString());
    Object.entries(changes).forEach(([key, value]) => {
      if (value === undefined || value === "") next.delete(key);
      else next.set(key, String(value));
    });
    if (!Object.hasOwn(changes, "page")) next.set("page", "0");
    const query = next.toString();
    router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
  }, [pathname, router, searchParams]);

  useEffect(() => {
    const current = filters.orderNumber ?? "";
    const next = searchValue.trim();
    if (current === next) return;
    const timer = window.setTimeout(() => updateParams({ search: next }), 350);
    return () => window.clearTimeout(timer);
  }, [filters.orderNumber, searchValue, updateParams]);

  const activeFilters = Boolean(filters.orderNumber || filters.status || filters.sku || filters.createdFrom || filters.createdTo);
  const updateFilter = (key: "status" | "sku" | "createdFrom" | "createdTo", value: string) => {
    if (key === "status") updateParams({ status: value && ORDER_STATUSES.includes(value as (typeof ORDER_STATUSES)[number]) ? value : undefined });
    else updateParams({ [key]: key === "sku" ? value.trim().toUpperCase() : value });
  };

  return <><PageIntro eyebrow="Order operations" title="Orders" description="Search and operate on historical orders without losing their purchase-time snapshots." /><OrderFilters values={filters} searchValue={searchValue} onSearchChange={setSearchValue} onChange={updateFilter} /><div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><p className="text-sm text-slate-500">{orders.data ? `${orders.data.totalElements} order${orders.data.totalElements === 1 ? "" : "s"}` : "Loading orders…"}{activeFilters && <span className="ml-2 text-slate-400">· Filters applied</span>}</p><OrderSortControl value={filters.sort} onChange={(sort) => updateParams({ sort })} /></div><div className="mt-4">{orders.isLoading ? <LoadingCard rows={7} /> : orders.isError ? <ErrorState title="Orders are unavailable" message={errorMessage(orders.error)} onRetry={() => void orders.refetch()} /> : orders.data?.content.length === 0 ? <EmptyState title="No orders found" message={activeFilters ? "No orders match these filters." : "No orders have been recorded yet."} action={activeFilters ? <Link href="/orders" className="text-sm font-semibold text-primary hover:underline">Clear filters</Link> : undefined} /> : orders.data && <OrderTable page={orders.data} pageSize={filters.size} onPageChange={(page) => updateParams({ page })} onPageSizeChange={(size) => updateParams({ size, page: 0 })} />}</div></>;
}
