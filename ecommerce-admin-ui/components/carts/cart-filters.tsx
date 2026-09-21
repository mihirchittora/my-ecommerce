import { ListFilter, Search } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { CART_STATUSES, type CartListParams, type CartSort, type CartStatus } from "@/lib/api/cart/types";
import { titleCase } from "@/lib/utils";

type FilterValues = Pick<CartListParams, "status" | "sku">;

export function CartFilters({ values, searchValue, onSearchChange, onChange }: {
  values: FilterValues;
  searchValue: string;
  onSearchChange: (value: string) => void;
  onChange: (key: "status" | "sku", value: string) => void;
}) {
  return <Card><CardContent className="p-4"><div className="grid gap-3 md:grid-cols-[1.4fr_1fr_1fr]"><label className="relative block"><span className="sr-only">Search by Cart ID or customer reference</span><Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><Input className="pl-9" placeholder="Cart ID or customer reference" value={searchValue} onChange={(event) => onSearchChange(event.target.value)} /></label><label className="relative block"><span className="sr-only">Filter by status</span><ListFilter className="pointer-events-none absolute left-3 top-1/2 z-10 h-4 w-4 -translate-y-1/2 text-slate-400" /><Select className="pl-9" aria-label="Filter carts by status" value={values.status ?? ""} onChange={(event) => onChange("status", event.target.value)}><option value="">All statuses</option>{CART_STATUSES.map((status) => <option key={status} value={status}>{titleCase(status)}</option>)}</Select></label><label><span className="sr-only">Filter by SKU</span><Input className="uppercase" placeholder="SKU" value={values.sku ?? ""} onChange={(event) => onChange("sku", event.target.value)} /></label></div><p className="mt-3 text-xs text-slate-400">Search supports an exact Cart ID or a customer reference fragment. Customer email is not exposed by the current Cart API.</p></CardContent></Card>;
}

export function CartSortControl({ value, onChange }: { value: CartSort; onChange: (value: CartSort) => void }) {
  return <label className="flex items-center gap-2 text-sm text-slate-500"><span className="whitespace-nowrap">Sort by</span><Select aria-label="Sort carts" value={value} onChange={(event) => onChange(event.target.value as CartSort)}><option value="updatedAt,desc">Recently updated</option><option value="updatedAt,asc">Least recently updated</option><option value="createdAt,desc">Newest</option><option value="createdAt,asc">Oldest</option><option value="expiresAt,asc">Expires soonest</option><option value="status,asc">Status</option><option value="currency,asc">Currency</option></Select></label>;
}

export function isCartStatusValue(value: string): value is CartStatus {
  return CART_STATUSES.includes(value as CartStatus);
}
