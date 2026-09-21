"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { CartFilters, CartSortControl, isCartStatusValue } from "@/components/carts/cart-filters";
import { CartTable } from "@/components/carts/cart-table";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";
import { ApiError } from "@/lib/api/client";
import { CART_STATUSES, type CartListParams, type CartSort } from "@/lib/api/cart/types";
import { useCartList } from "@/lib/api/cart/queries";

const DEFAULT_SIZE = 20;
const DEFAULT_SORT: CartSort = "updatedAt,desc";
const SORTS: CartSort[] = ["updatedAt,desc", "updatedAt,asc", "createdAt,desc", "createdAt,asc", "expiresAt,asc", "expiresAt,desc", "status,asc", "status,desc", "currency,asc", "currency,desc"];

function readPage(value: string | null) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
}

function readSize(value: string | null) {
  return value === "50" || value === "100" ? Number(value) : DEFAULT_SIZE;
}

function readSort(value: string | null): CartSort {
  return value && SORTS.includes(value as CartSort) ? value as CartSort : DEFAULT_SORT;
}

function readFilters(searchParams: ReturnType<typeof useSearchParams>): CartListParams {
  const status = searchParams.get("status");
  return {
    page: readPage(searchParams.get("page")),
    size: readSize(searchParams.get("size")),
    sort: readSort(searchParams.get("sort")),
    search: searchParams.get("search") || undefined,
    status: status && isCartStatusValue(status) ? status : undefined,
    sku: searchParams.get("sku") || undefined,
  };
}

function errorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 403) return "You do not have permission to view customer carts.";
    if (error.status === 503 || error.status === 0) return "Cart service is temporarily unavailable.";
    return error.message;
  }
  return "Cart service is temporarily unavailable.";
}

export default function CartsPage() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const filters = useMemo(() => readFilters(searchParams), [searchParams]);
  const [searchValue, setSearchValue] = useState(filters.search ?? "");
  const carts = useCartList(filters);

  useEffect(() => setSearchValue(filters.search ?? ""), [filters.search]);

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
    const current = filters.search ?? "";
    const next = searchValue.trim();
    if (current === next) return;
    const timer = window.setTimeout(() => updateParams({ search: next }), 350);
    return () => window.clearTimeout(timer);
  }, [filters.search, searchValue, updateParams]);

  const activeFilters = Boolean(filters.search || filters.status || filters.sku);
  const updateFilter = (key: "status" | "sku", value: string) => {
    if (key === "status") updateParams({ status: value && CART_STATUSES.includes(value as (typeof CART_STATUSES)[number]) ? value : undefined });
    else updateParams({ sku: value.trim().toUpperCase() });
  };

  return <><PageIntro eyebrow="Commerce operations" title="Carts" description="Inspect customer-owned SKU selections and their current Catalog and Inventory context. Cart quantities do not reserve stock." /><CartFilters values={filters} searchValue={searchValue} onSearchChange={setSearchValue} onChange={updateFilter} /><div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><p className="text-sm text-slate-500">{carts.data ? `${carts.data.totalElements} cart${carts.data.totalElements === 1 ? "" : "s"}` : "Loading carts…"}{activeFilters && <span className="ml-2 text-slate-400">· Filters applied</span>}</p><CartSortControl value={filters.sort} onChange={(sort) => updateParams({ sort })} /></div><div className="mt-4">{carts.isLoading ? <LoadingCard rows={7} /> : carts.isError ? <ErrorState title="Carts are unavailable" message={errorMessage(carts.error)} onRetry={() => void carts.refetch()} /> : carts.data?.content.length === 0 ? <EmptyState title={activeFilters ? "No carts match your filters" : "No carts found"} message={activeFilters ? "Try clearing one or more filters." : "No customer carts have been recorded yet."} action={activeFilters ? <Link href="/carts" className="text-sm font-semibold text-primary hover:underline">Clear filters</Link> : undefined} /> : carts.data && <CartTable page={carts.data} pageSize={filters.size} onPageChange={(page) => updateParams({ page })} onPageSizeChange={(size) => updateParams({ size, page: 0 })} />}</div></>;
}
