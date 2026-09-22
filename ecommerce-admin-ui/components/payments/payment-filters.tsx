import { CalendarDays, ListFilter, Search } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { PAYMENT_PROVIDERS, PAYMENT_STATUSES, type PaymentListParams, type PaymentProvider, type PaymentStatus } from "@/lib/api/payment/types";
import { titleCase } from "@/lib/utils";

type FilterValues = Pick<PaymentListParams, "status" | "provider" | "currency" | "createdFrom" | "createdTo">;
type FilterKey = keyof FilterValues;

export function PaymentFilters({ values, searchValue, onSearchChange, onChange }: { values: FilterValues; searchValue: string; onSearchChange: (value: string) => void; onChange: (key: FilterKey, value: string) => void }) {
  return <Card><CardContent className="p-4"><div className="grid gap-3 lg:grid-cols-[1.6fr_1fr_1fr_1fr_1fr]"><label className="relative block lg:col-span-2"><span className="sr-only">Search payment references</span><Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><Input className="pl-9" placeholder="Payment, order, customer, or provider reference" value={searchValue} onChange={(event) => onSearchChange(event.target.value)} /></label><label className="relative block"><span className="sr-only">Filter by payment status</span><ListFilter className="pointer-events-none absolute left-3 top-1/2 z-10 h-4 w-4 -translate-y-1/2 text-slate-400" /><Select className="pl-9" aria-label="Filter payments by status" value={values.status ?? ""} onChange={(event) => onChange("status", event.target.value)}><option value="">All statuses</option>{PAYMENT_STATUSES.map((status) => <option key={status} value={status}>{titleCase(status)}</option>)}</Select></label><label><span className="sr-only">Filter by provider</span><Select aria-label="Filter payments by provider" value={values.provider ?? ""} onChange={(event) => onChange("provider", event.target.value)}><option value="">All providers</option>{PAYMENT_PROVIDERS.map((provider) => <option key={provider} value={provider}>{titleCase(provider)}</option>)}</Select></label><label><span className="sr-only">Filter by currency</span><Select aria-label="Filter payments by currency" value={values.currency ?? ""} onChange={(event) => onChange("currency", event.target.value)}><option value="">All currencies</option><option value="INR">INR</option><option value="USD">USD</option><option value="EUR">EUR</option></Select></label><label className="relative block"><span className="sr-only">Created from</span><CalendarDays className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><Input className="pl-9" type="date" aria-label="Payments created from" value={values.createdFrom ?? ""} onChange={(event) => onChange("createdFrom", event.target.value)} /></label><label className="relative block"><span className="sr-only">Created to</span><CalendarDays className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><Input className="pl-9" type="date" aria-label="Payments created to" value={values.createdTo ?? ""} onChange={(event) => onChange("createdTo", event.target.value)} /></label></div></CardContent></Card>;
}

export function isPaymentFilterStatus(value: string): value is PaymentStatus {
  return PAYMENT_STATUSES.includes(value as PaymentStatus);
}

export function isPaymentFilterProvider(value: string): value is PaymentProvider {
  return PAYMENT_PROVIDERS.includes(value as PaymentProvider);
}
