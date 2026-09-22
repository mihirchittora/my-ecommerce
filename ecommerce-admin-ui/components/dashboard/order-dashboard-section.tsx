"use client";

import Link from "next/link";
import { ClipboardList, Clock3, DollarSign, PackageCheck } from "lucide-react";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { useAuth } from "@/components/auth-provider";
import { DashboardMetricCard } from "@/components/dashboard/dashboard-metric-card";
import { CustomerReference } from "@/components/customers/customer-reference";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useOrderList } from "@/lib/api/order/queries";
import { OrderStatusBadge } from "@/components/orders/order-status-badge";
import { hasPermission } from "@/lib/permissions";
import { formatCurrency, formatDate } from "@/lib/utils";

export function OrderDashboardSection() {
  const { user } = useAuth();
  const canReadOrders = hasPermission(user, "ORDER_READ");
  const recent = useOrderList({ page: 0, size: 5, sort: "createdAt,desc" }, canReadOrders);
  const total = useOrderList({ page: 0, size: 1, sort: "createdAt,desc" }, canReadOrders);
  const pendingReservation = useOrderList({ page: 0, size: 1, sort: "createdAt,desc", status: "PENDING_RESERVATION" }, canReadOrders);
  const pendingPayment = useOrderList({ page: 0, size: 1, sort: "createdAt,desc", status: "PENDING_PAYMENT" }, canReadOrders);
  const completed = useOrderList({ page: 0, size: 1, sort: "createdAt,desc", status: "COMPLETED" }, canReadOrders);

  if (!canReadOrders) return null;

  const retry = () => {
    void Promise.all([recent.refetch(), total.refetch(), pendingReservation.refetch(), pendingPayment.refetch(), completed.refetch()]);
  };

  return (
    <section className="mt-6">
      <div className="mb-4 flex items-end justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">Order service</p>
          <h2 className="mt-1 text-lg font-semibold text-slate-900">Order health</h2>
        </div>
        <Link href="/orders" className="text-sm font-semibold text-primary hover:underline">Open orders</Link>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <DashboardMetricCard label="All orders" value={total.data?.totalElements ?? "—"} icon={ClipboardList} tone="blue" href="/orders" />
        <DashboardMetricCard label="Awaiting reservation" value={pendingReservation.data?.totalElements ?? "—"} icon={Clock3} tone="amber" />
        <DashboardMetricCard label="Awaiting payment" value={pendingPayment.data?.totalElements ?? "—"} icon={DollarSign} tone="violet" />
        <DashboardMetricCard label="Completed" value={completed.data?.totalElements ?? "—"} icon={PackageCheck} tone="green" />
      </div>
      <div className="mt-4">
        {recent.isLoading ? <LoadingCard rows={4} /> : recent.isError ? <ErrorState title="Orders unavailable" message="The Order service could not provide dashboard data." onRetry={retry} /> : (
          <Card className="overflow-hidden">
            <CardHeader className="flex-row items-center justify-between">
              <div>
                <CardTitle>Recent orders</CardTitle>
                <p className="mt-1 text-sm text-muted-foreground">Latest commercial records and their current checkout state.</p>
              </div>
              <ClipboardList className="h-5 w-5 text-slate-300" />
            </CardHeader>
            <CardContent className="p-0">
              {recent.data?.content.length ? (
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[760px] text-left">
                    <thead><tr className="border-y border-slate-100 text-xs font-semibold uppercase tracking-[0.1em] text-slate-400"><th className="px-5 py-3">Order</th><th className="px-3 py-3">Status</th><th className="px-3 py-3 text-right">Total</th><th className="px-3 py-3">Customer</th><th className="px-3 py-3">Created</th></tr></thead>
                    <tbody>{recent.data.content.map((order) => <tr className="table-row" key={order.id}><td className="px-5 py-4"><Link href={`/orders/${order.id}`} className="font-mono text-sm font-semibold text-primary hover:underline">{order.orderNumber}</Link></td><td className="px-3 py-4"><OrderStatusBadge status={order.status} /></td><td className="px-3 py-4 text-right text-sm font-semibold text-slate-800">{formatCurrency(order.totalAmount, order.currency)}</td><td className="px-3 py-4"><CustomerReference customerId={order.customerId} /></td><td className="px-3 py-4 text-sm text-slate-600">{formatDate(order.createdAt)}</td></tr>)}</tbody>
                  </table>
                </div>
              ) : <p className="p-5 text-sm text-slate-500">No orders have been recorded yet.</p>}
            </CardContent>
          </Card>
        )}
      </div>
    </section>
  );
}
