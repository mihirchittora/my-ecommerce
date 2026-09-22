"use client";

import Link from "next/link";
import { useAuth } from "@/components/auth-provider";
import { useCustomer } from "@/lib/api/customer/queries";

function customerDisplayName(customer: { firstName: string | null; lastName: string | null; email: string | null }) {
  return [customer.firstName, customer.lastName].filter(Boolean).join(" ").trim() || customer.email || "Unnamed customer";
}

export function CustomerReference({ customerId }: { customerId: string }) {
  const { hasPermission } = useAuth();
  const canReadCustomer = hasPermission("CUSTOMER_READ");
  const customer = useCustomer(customerId, canReadCustomer);
  const name = customer.isLoading ? "Loading customer…" : customer.data ? customerDisplayName(customer.data) : "Customer unavailable";

  return <div className="min-w-0" title={customerId}>
    {canReadCustomer ? <Link href={`/customers/${customerId}`} className="block max-w-[15rem] truncate text-sm font-semibold text-primary hover:underline">{name}</Link> : <span className="block text-sm font-medium text-slate-700">Customer details unavailable</span>}
    <span className="mt-1 block max-w-[15rem] truncate font-mono text-[11px] text-slate-400">{customer.data?.email ?? customerId}</span>
  </div>;
}
