import { CalendarDays, ListFilter, Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Card, CardContent } from "@/components/ui/card";
import { ORDER_STATUSES, type OrderListParams, type OrderSort, type OrderStatus } from "@/lib/api/order/types";
import { titleCase } from "@/lib/utils";

type FilterValues = Pick<OrderListParams, "orderNumber" | "status" | "sku" | "createdFrom" | "createdTo">;
type FilterKey = Exclude<keyof FilterValues, "orderNumber">;

export function OrderFilters({ values, searchValue, onSearchChange, onChange }: { values: FilterValues; searchValue: string; onSearchChange: (value: string) => void; onChange: (key: FilterKey, value: string) => void }) {
  return <Card><CardContent className="p-4"><div className="grid gap-3 lg:grid-cols-[1.4fr_1fr_1fr_1fr_1fr]"><label className="relative block"><span className="sr-only">Search by order number</span><Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><Input className="pl-9" placeholder="Search order number" value={searchValue} onChange={(event) => onSearchChange(event.target.value)} /></label><label className="relative block"><span className="sr-only">Filter by status</span><ListFilter className="pointer-events-none absolute left-3 top-1/2 z-10 h-4 w-4 -translate-y-1/2 text-slate-400" /><Select className="pl-9" aria-label="Filter by status" value={values.status ?? ""} onChange={(event) => onChange("status", event.target.value)}><option value="">All statuses</option>{ORDER_STATUSES.map((status) => <option key={status} value={status}>{titleCase(status)}</option>)}</Select></label><label><span className="sr-only">Filter by SKU</span><Input className="uppercase" placeholder="SKU" value={values.sku ?? ""} onChange={(event) => onChange("sku", event.target.value)} /></label><label className="relative block"><span className="sr-only">Created from</span><CalendarDays className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><Input className="pl-9" type="date" aria-label="Created from" value={values.createdFrom ?? ""} onChange={(event) => onChange("createdFrom", event.target.value)} /></label><label className="relative block"><span className="sr-only">Created to</span><CalendarDays className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><Input className="pl-9" type="date" aria-label="Created to" value={values.createdTo ?? ""} onChange={(event) => onChange("createdTo", event.target.value)} /></label></div></CardContent></Card>;
}

export function OrderSortControl({ value, onChange }: { value: OrderSort; onChange: (value: OrderSort) => void }) {
  return <label className="flex items-center gap-2 text-sm text-slate-500"><span className="whitespace-nowrap">Sort by</span><Select aria-label="Sort orders" value={value} onChange={(event) => onChange(event.target.value as OrderSort)}><option value="createdAt,desc">Newest</option><option value="createdAt,asc">Oldest</option><option value="updatedAt,desc">Recently updated</option><option value="orderNumber,asc">Order number</option><option value="totalAmount,desc">Highest total</option><option value="totalAmount,asc">Lowest total</option><option value="status,asc">Status</option></Select></label>;
}

export function isOrderStatus(value: string): value is OrderStatus {
  return ORDER_STATUSES.includes(value as OrderStatus);
}
