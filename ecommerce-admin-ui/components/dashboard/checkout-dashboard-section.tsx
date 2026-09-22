"use client";

import Link from "next/link";
import { CheckCircle2, CircleDashed, Clock3, ShoppingCart, Timer } from "lucide-react";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { useAuth } from "@/components/auth-provider";
import { DashboardMetricCard } from "@/components/dashboard/dashboard-metric-card";
import { CustomerReference } from "@/components/customers/customer-reference";
import { CartStatusBadge } from "@/components/carts/cart-status-badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useCartList } from "@/lib/api/cart/queries";
import { hasPermission } from "@/lib/permissions";
import { formatDate } from "@/lib/utils";

export function CheckoutDashboardSection() {
  const { user } = useAuth();
  const canReadCarts = hasPermission(user, "CART_READ");
  const recent = useCartList({ page: 0, size: 5, sort: "updatedAt,desc" }, canReadCarts);
  const active = useCartList({ page: 0, size: 1, sort: "updatedAt,desc", status: "ACTIVE" }, canReadCarts);
  const checkoutInProgress = useCartList({ page: 0, size: 1, sort: "updatedAt,desc", status: "CHECKOUT_IN_PROGRESS" }, canReadCarts);
  const converted = useCartList({ page: 0, size: 1, sort: "updatedAt,desc", status: "CONVERTED" }, canReadCarts);
  const abandoned = useCartList({ page: 0, size: 1, sort: "updatedAt,desc", status: "ABANDONED" }, canReadCarts);
  const expired = useCartList({ page: 0, size: 1, sort: "updatedAt,desc", status: "EXPIRED" }, canReadCarts);

  if (!canReadCarts) return null;

  const retry = () => {
    void Promise.all([recent.refetch(), active.refetch(), checkoutInProgress.refetch(), converted.refetch(), abandoned.refetch(), expired.refetch()]);
  };

  return (
    <section className="mt-6">
      <div className="mb-4 flex items-end justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">Checkout service</p>
          <h2 className="mt-1 text-lg font-semibold text-slate-900">Checkout lifecycle</h2>
        </div>
        <Link href="/carts" className="text-sm font-semibold text-primary hover:underline">Open carts</Link>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <DashboardMetricCard label="Active carts" value={active.data?.totalElements ?? "—"} icon={ShoppingCart} tone="blue" href="/carts?status=ACTIVE" />
        <DashboardMetricCard label="Checkout in progress" value={checkoutInProgress.data?.totalElements ?? "—"} icon={Clock3} tone="amber" href="/carts?status=CHECKOUT_IN_PROGRESS" />
        <DashboardMetricCard label="Converted" value={converted.data?.totalElements ?? "—"} icon={CheckCircle2} tone="green" href="/carts?status=CONVERTED" />
        <DashboardMetricCard label="Abandoned" value={abandoned.data?.totalElements ?? "—"} icon={CircleDashed} tone="violet" href="/carts?status=ABANDONED" />
        <DashboardMetricCard label="Expired" value={expired.data?.totalElements ?? "—"} icon={Timer} tone="amber" href="/carts?status=EXPIRED" />
      </div>
      <div className="mt-4">
        {recent.isLoading ? <LoadingCard rows={4} /> : recent.isError ? <ErrorState title="Checkout data unavailable" message="The Cart service could not provide checkout dashboard data." onRetry={retry} /> : (
          <Card className="overflow-hidden">
            <CardHeader className="flex-row items-center justify-between">
              <div>
                <CardTitle>Recent checkout activity</CardTitle>
                <p className="mt-1 text-sm text-muted-foreground">Cart state, item volume and conversion links from the Cart service.</p>
              </div>
              <ShoppingCart className="h-5 w-5 text-slate-300" />
            </CardHeader>
            <CardContent className="p-0">
              {recent.data?.content.length ? (
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[820px] text-left">
                    <thead><tr className="border-y border-slate-100 text-xs font-semibold uppercase tracking-[0.1em] text-slate-400"><th className="px-5 py-3">Cart</th><th className="px-3 py-3">Customer</th><th className="px-3 py-3">Status</th><th className="px-3 py-3 text-right">Items</th><th className="px-3 py-3 text-right">Quantity</th><th className="px-3 py-3">Updated</th><th className="px-3 py-3">Order</th></tr></thead>
                    <tbody>{recent.data.content.map((cart) => <tr className="table-row" key={cart.id}><td className="px-5 py-4"><Link href={`/carts/${cart.id}`} className="font-mono text-sm font-semibold text-primary hover:underline">{cart.id.slice(0, 8)}…</Link></td><td className="px-3 py-4"><CustomerReference customerId={cart.customerId} /></td><td className="px-3 py-4"><CartStatusBadge status={cart.status} /></td><td className="px-3 py-4 text-right text-sm text-slate-700">{cart.itemCount}</td><td className="px-3 py-4 text-right text-sm text-slate-700">{cart.totalQuantity}</td><td className="px-3 py-4 text-sm text-slate-600">{formatDate(cart.updatedAt)}</td><td className="px-3 py-4">{cart.convertedOrderId ? <Link href={`/orders/${cart.convertedOrderId}`} className="font-mono text-xs font-semibold text-primary hover:underline">{cart.convertedOrderNumber ?? "View order"}</Link> : <span className="text-sm text-slate-400">—</span>}</td></tr>)}</tbody>
                  </table>
                </div>
              ) : <p className="p-5 text-sm text-slate-500">No carts have been recorded yet.</p>}
            </CardContent>
          </Card>
        )}
      </div>
    </section>
  );
}
