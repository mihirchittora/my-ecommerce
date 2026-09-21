"use client";

import Link from "next/link";
import { Eye, Search, Users } from "lucide-react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback, useMemo, useState } from "react";
import { AdminPagination } from "@/components/auth-admin/admin-pagination";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { ApiError } from "@/lib/api/client";
import { CUSTOMER_STATUSES } from "@/lib/api/customer/types";
import type { CustomerStatus } from "@/lib/api/customer/types";
import { useCustomers } from "@/lib/api/customer/queries";
import { formatDate } from "@/lib/utils";

const PAGE_SIZE = 20;

function pageFromQuery(value: string | null) {
  const page = Number(value);
  return Number.isInteger(page) && page >= 0 ? page : 0;
}

function statusVariant(status: CustomerStatus) {
  return status === "ACTIVE" ? "success" : status === "BLOCKED" ? "danger" : "muted";
}

function errorMessage(error: unknown) {
  if (error instanceof ApiError && error.status === 403) return "You do not have CUSTOMER_READ permission.";
  if (error instanceof ApiError && (error.status === 0 || error.status >= 500)) return "Customer Service is temporarily unavailable.";
  return error instanceof ApiError ? error.message : "Customer Service is temporarily unavailable.";
}

export default function CustomersPage() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const page = useMemo(() => pageFromQuery(searchParams.get("page")), [searchParams]);
  const [search, setSearch] = useState(searchParams.get("search") ?? "");
  const [status, setStatus] = useState<CustomerStatus | "">((searchParams.get("status") as CustomerStatus | "") || "");
  const customers = useCustomers({ page, size: PAGE_SIZE, search: searchParams.get("search") ?? undefined, status: (searchParams.get("status") as CustomerStatus | undefined) || undefined });
  const pageData = customers.data && Array.isArray(customers.data.content) ? customers.data : null;

  const updateQuery = useCallback((next: { page?: number; search?: string; status?: string }) => {
    const params = new URLSearchParams(searchParams.toString());
    if (next.page !== undefined) {
      if (next.page > 0) params.set("page", String(next.page)); else params.delete("page");
    }
    if (next.search !== undefined) {
      if (next.search.trim()) params.set("search", next.search.trim()); else params.delete("search");
    }
    if (next.status !== undefined) {
      if (next.status) params.set("status", next.status); else params.delete("status");
    }
    router.replace(`${pathname}${params.toString() ? `?${params}` : ""}`, { scroll: false });
  }, [pathname, router, searchParams]);

  const submitSearch = (event: React.FormEvent<HTMLFormElement>) => { event.preventDefault(); updateQuery({ page: 0, search, status }); };

  return <><PageIntro eyebrow="User Management" title="Customers" description="View customer profiles and their shipping and billing addresses. Customer registration remains a public Auth flow; this screen manages existing customer records." /><Card className="mb-5"><CardContent className="p-4"><form className="grid gap-3 md:grid-cols-[1fr_180px_auto]" onSubmit={submitSearch}><div className="relative"><Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" /><Input className="pl-9" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search name, email, or Auth ID" aria-label="Search customers" /></div><Select value={status} onChange={(event) => { const next = event.target.value as CustomerStatus | ""; setStatus(next); updateQuery({ page: 0, search, status: next }); }} aria-label="Filter by customer status"><option value="">All statuses</option>{CUSTOMER_STATUSES.map((value) => <option value={value} key={value}>{value}</option>)}</Select><Button type="submit"><Search className="h-4 w-4" />Search</Button></form></CardContent></Card>{customers.isLoading ? <LoadingCard rows={7} /> : customers.isError ? <ErrorState title="Customers are unavailable" message={errorMessage(customers.error)} onRetry={() => void customers.refetch()} /> : !pageData ? <ErrorState title="Customers are unavailable" message="Customer Service returned an invalid customer list response." onRetry={() => void customers.refetch()} /> : pageData.content.length === 0 ? <EmptyState title="No customers found" message="No customer profiles match the current search or status filter." action={<Users className="h-5 w-5 text-slate-400" />} /> : <Card className="overflow-hidden"><CardContent className="p-0"><div className="hidden overflow-x-auto md:block"><table className="w-full min-w-[920px] text-left"><caption className="sr-only">Customer profiles</caption><thead><tr className="border-y border-slate-100 text-xs font-semibold uppercase tracking-[0.1em] text-slate-400"><th className="px-5 py-3">Customer</th><th className="px-3 py-3">Email</th><th className="px-3 py-3">Phone</th><th className="px-3 py-3">Status</th><th className="px-3 py-3">Created</th><th className="px-3 py-3 text-right">Actions</th></tr></thead><tbody>{pageData.content.map((customer) => <tr className="table-row" key={customer.id}><td className="px-5 py-4"><Link href={`/customers/${customer.id}`} className="font-semibold text-slate-800 hover:text-primary">{`${customer.firstName ?? ""} ${customer.lastName ?? ""}`.trim() || "Unnamed customer"}</Link><p className="mt-1 font-mono text-[11px] text-slate-400">{customer.id}</p></td><td className="px-3 py-4 text-sm text-slate-600">{customer.email || "Not synced"}</td><td className="px-3 py-4 text-sm text-slate-600">{customer.phone || "—"}</td><td className="px-3 py-4"><Badge variant={statusVariant(customer.status)}>{customer.status}</Badge></td><td className="px-3 py-4 text-sm text-slate-600">{formatDate(customer.createdAt)}</td><td className="px-3 py-4 text-right"><Button asChild variant="ghost" size="sm"><Link href={`/customers/${customer.id}`}><Eye className="h-4 w-4" />View</Link></Button></td></tr>)}</tbody></table></div><div className="divide-y divide-slate-100 md:hidden">{pageData.content.map((customer) => <article className="space-y-3 p-5" key={customer.id}><div className="flex items-start justify-between gap-3"><Link href={`/customers/${customer.id}`} className="font-semibold text-slate-800 hover:text-primary">{`${customer.firstName ?? ""} ${customer.lastName ?? ""}`.trim() || "Unnamed customer"}</Link><Badge variant={statusVariant(customer.status)}>{customer.status}</Badge></div><p className="text-sm text-slate-600">{customer.email || "Email is owned by Auth"}</p><p className="font-mono text-[11px] text-slate-400">{customer.id}</p><Button asChild variant="outline" size="sm"><Link href={`/customers/${customer.id}`}><Eye className="h-4 w-4" />View profile</Link></Button></article>)}</div><AdminPagination page={pageData} onPageChange={(nextPage) => updateQuery({ page: nextPage })} /></CardContent></Card>}</>;
}
