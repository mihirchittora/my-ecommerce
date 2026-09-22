"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useParams } from "next/navigation";
import { useMemo } from "react";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { OrderActions } from "@/components/orders/order-actions";
import { OrderPaymentPanel } from "@/components/orders/order-payment-panel";
import { OrderFulfillmentPanel, OrderHistoryTimeline, OrderInventoryPanel, OrderItems, OrderSummary } from "@/components/orders/order-detail-sections";
import { OrderStatusBadge } from "@/components/orders/order-status-badge";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/client";
import { useOrder, useOrderReservations } from "@/lib/api/order/queries";
import { formatDate } from "@/lib/utils";

function errorState(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 403) return { title: "Order access denied", message: "You do not have permission to view orders." };
    if (error.status === 404) return { title: "Order not found", message: "The order may have been removed or the link may be invalid." };
    if (error.status === 0 || error.status >= 500) return { title: "Order service unavailable", message: "Order service is temporarily unavailable." };
    return { title: "Unable to load order", message: error.message };
  }
  return { title: "Unable to load order", message: "Order service is temporarily unavailable." };
}

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const order = useOrder(id);
  const reservationQueries = useOrderReservations(order.data?.items ?? []);
  const reservationStates = useMemo(() => (order.data?.items ?? []).map((item) => {
    const query = item.reservationId ? reservationQueries.find((entry) => entry.id === item.reservationId)?.query : undefined;
    return { item, reservation: query?.data, isLoading: query?.isPending ?? false, error: query?.error };
  }), [order.data?.items, reservationQueries]);
  const retryInventory = () => void Promise.all(reservationQueries.map((entry) => entry.query.refetch()));

  if (order.isLoading) return <LoadingCard rows={8} />;
  if (order.isError || !order.data) {
    const error = errorState(order.error);
    return <ErrorState title={error.title} message={error.message} onRetry={() => void order.refetch()} />;
  }

  const itemCount = order.data.items.reduce((total, item) => total + item.quantity, 0);
  return <><div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/orders"><ArrowLeft className="h-4 w-4" />Back to orders</Link></Button></div><div className="mb-8 flex flex-col gap-4 md:flex-row md:items-end md:justify-between"><div><p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">Order detail</p><div className="flex flex-wrap items-center gap-3"><h1 className="font-mono text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">{order.data.orderNumber}</h1><OrderStatusBadge status={order.data.status} /></div><p className="mt-2 text-sm text-slate-500">{itemCount} item{itemCount === 1 ? "" : "s"} · Created {formatDate(order.data.createdAt)}</p></div><OrderActions order={order.data} /></div><div className="space-y-6"><OrderSummary order={order.data} /><OrderFulfillmentPanel orderId={order.data.id} /><OrderPaymentPanel order={order.data} /><OrderItems items={order.data.items} /><OrderInventoryPanel states={reservationStates} onRetry={retryInventory} /><OrderHistoryTimeline entries={order.data.history} /></div></>;
}
