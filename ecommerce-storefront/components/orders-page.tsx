"use client";

import { useQuery } from "@tanstack/react-query";
import { orderApi } from "@/lib/api/order";
import { RequireAuth } from "@/components/require-auth";
import { OrderCard } from "@/components/order-card";
import { EmptyState, ErrorState, LoadingBlock } from "@/components/feedback";
import { Button } from "@/components/ui/button";

function OrdersContents() {
  const orders = useQuery({ queryKey: ["orders", "mine", 0], queryFn: () => orderApi.listMine(0) });
  if (orders.isLoading) return <LoadingBlock label="Loading your orders" />;
  if (orders.isError) return <ErrorState message="Your orders are temporarily unavailable." retry={() => void orders.refetch()} />;
  return <div><p className="eyebrow">Your purchases</p><h1 className="mt-3 font-display text-4xl font-bold tracking-tight md:text-6xl">Order history</h1><p className="mt-5 max-w-xl text-base leading-7 text-ink/60">Every order is shown with the price and address snapshot used at checkout.</p><div className="mt-10 grid gap-4">{orders.data?.content.length ? orders.data.content.map((order) => <OrderCard key={order.id} order={order} />) : <EmptyState title="You haven't placed any orders yet" message="Your future purchases will appear here once an order is created." action={<Button asLink="/products">Start shopping</Button>} />}</div>{orders.data && orders.data.totalPages > 1 ? <p className="mt-7 text-sm text-ink/55">Showing page {orders.data.number + 1} of {orders.data.totalPages}. More pages can be loaded from the order service.</p> : null}</div>;
}

export default function OrdersPage() { return <div className="page-shell py-10 md:py-16"><RequireAuth><OrdersContents /></RequireAuth></div>; }
