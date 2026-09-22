"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { PaymentFilters, isPaymentFilterMethod, isPaymentFilterProvider, isPaymentFilterStatus } from "@/components/payments/payment-filters";
import { PaymentTable } from "@/components/payments/payment-table";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";
import { ApiError } from "@/lib/api/client";
import { isPaymentSort } from "@/lib/api/payment/payments";
import { usePayments } from "@/lib/api/payment/queries";
import { PAYMENT_STATUSES, type PaymentListParams, type PaymentSort } from "@/lib/api/payment/types";

const DEFAULT_SIZE = 20;
const DEFAULT_SORT: PaymentSort = "createdAt,desc";

function readPage(value: string | null) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
}

function readSize(value: string | null) {
  return value === "50" || value === "100" ? Number(value) : DEFAULT_SIZE;
}

function readSort(value: string | null): PaymentSort {
  return value && isPaymentSort(value) ? value : DEFAULT_SORT;
}

function readFilters(searchParams: ReturnType<typeof useSearchParams>): PaymentListParams {
  const status = searchParams.get("status");
  const provider = searchParams.get("provider");
  const paymentMethod = searchParams.get("paymentMethod");
  return {
    page: readPage(searchParams.get("page")),
    size: readSize(searchParams.get("size")),
    sort: readSort(searchParams.get("sort")),
    search: searchParams.get("search") || undefined,
    status: status && isPaymentFilterStatus(status) ? status : undefined,
    provider: provider && isPaymentFilterProvider(provider) ? provider : undefined,
    paymentMethod: paymentMethod && isPaymentFilterMethod(paymentMethod) ? paymentMethod : undefined,
    currency: searchParams.get("currency") || undefined,
    createdFrom: searchParams.get("createdFrom") || undefined,
    createdTo: searchParams.get("createdTo") || undefined,
  };
}

function errorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 403) return "You do not have permission to view payments.";
    if (error.status === 503 || error.status === 0) return "Payment Service is temporarily unavailable.";
    return error.message;
  }
  return "Payment Service is temporarily unavailable.";
}

export default function PaymentsPage() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const filters = useMemo(() => readFilters(searchParams), [searchParams]);
  const [searchValue, setSearchValue] = useState(filters.search ?? "");
  const payments = usePayments(filters);

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

  const activeFilters = Boolean(filters.search || filters.status || filters.provider || filters.paymentMethod || filters.currency || filters.createdFrom || filters.createdTo);
  const updateFilter = (key: "status" | "provider" | "paymentMethod" | "currency" | "createdFrom" | "createdTo", value: string) => {
    if (key === "status") updateParams({ status: value && PAYMENT_STATUSES.includes(value as (typeof PAYMENT_STATUSES)[number]) ? value : undefined });
    else if (key === "paymentMethod") updateParams({ paymentMethod: value || undefined });
    else updateParams({ [key]: value });
  };

  return <><PageIntro eyebrow="Payment operations" title="Payments" description="Monitor online payments and cash-on-delivery collection without crossing Order or Customer ownership boundaries." /><PaymentFilters values={filters} searchValue={searchValue} onSearchChange={setSearchValue} onChange={updateFilter} /><div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><p className="text-sm text-slate-500">{payments.data ? `${payments.data.totalElements} payment${payments.data.totalElements === 1 ? "" : "s"}` : "Loading payments…"}{activeFilters && <span className="ml-2 text-slate-400">· Filters applied</span>}</p><PaymentSortControl value={filters.sort} onChange={(sort) => updateParams({ sort })} /></div><div className="mt-4">{payments.isLoading ? <LoadingCard rows={7} /> : payments.isError ? <ErrorState title="Payments are unavailable" message={errorMessage(payments.error)} onRetry={() => void payments.refetch()} /> : payments.data?.content.length === 0 ? <EmptyState title="No payments found" message={activeFilters ? "No payments match these filters." : "No payments have been recorded yet."} action={activeFilters ? <Link href="/payments" className="text-sm font-semibold text-primary hover:underline">Clear filters</Link> : undefined} /> : payments.data && <PaymentTable page={payments.data} pageSize={filters.size} onPageChange={(page) => updateParams({ page })} onPageSizeChange={(size) => updateParams({ size, page: 0 })} />}</div></>;
}

function PaymentSortControl({ value, onChange }: { value: PaymentSort; onChange: (value: PaymentSort) => void }) {
  return <label className="flex items-center gap-2 text-sm text-slate-500"><span className="whitespace-nowrap">Sort by</span><select aria-label="Sort payments" className="h-9 rounded-lg border border-input bg-background px-3" value={value} onChange={(event) => onChange(event.target.value as PaymentSort)}><option value="createdAt,desc">Newest</option><option value="createdAt,asc">Oldest</option><option value="updatedAt,desc">Recently updated</option><option value="amount,desc">Highest amount</option><option value="amount,asc">Lowest amount</option><option value="status,asc">Status</option><option value="provider,asc">Provider</option><option value="refundedAmount,desc">Highest refunded</option></select></label>;
}
