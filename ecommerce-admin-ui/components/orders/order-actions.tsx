"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useToast } from "@/components/ui/toast";
import { useAuth } from "@/components/auth-provider";
import { useCancelOrder } from "@/lib/api/order/queries";
import { getAllowedOrderActions } from "@/lib/api/order/action-rules";
import type { OrderDetail, OrderSummary } from "@/lib/api/order/types";
import { ApiError } from "@/lib/api/client";

type OrderActionTarget = OrderSummary | OrderDetail;

function errorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 403) return "You do not have permission to cancel this order.";
    if (error.status === 409) return "Order cannot be cancelled in its current state.";
    if (error.status === 503) return "Inventory service is unavailable; cancellation could not safely complete.";
    if (error.status === 404) return "Order was not found or is no longer available.";
    return error.message;
  }
  return "The order could not be cancelled. Please try again.";
}

export function OrderActions({ order }: { order: OrderActionTarget }) {
  const { user } = useAuth();
  const canCancel = getAllowedOrderActions(order, user?.permissions ?? []).includes("cancel");
  if (!canCancel) return null;
  return <CancelOrderDialog order={order} />;
}

export function CancelOrderDialog({ order }: { order: OrderActionTarget }) {
  const [open, setOpen] = useState(false);
  const cancel = useCancelOrder();
  const { toast } = useToast();
  const reservationIds = "items" in order ? order.items.flatMap((item) => item.reservationId ? [item.reservationId] : []) : undefined;
  const execute = () => cancel.mutate({ orderId: order.id, reservationIds }, {
    onSuccess: () => {
      setOpen(false);
      toast({ title: "Order cancelled", description: `${order.orderNumber} was cancelled.` });
    },
  });
  return <><Button variant="outline" size="sm" className="text-rose-600 hover:text-rose-700" onClick={() => setOpen(true)}>Cancel order</Button><Dialog open={open} onOpenChange={(nextOpen) => { if (!cancel.isPending) setOpen(nextOpen); }}><DialogContent><DialogHeader><DialogTitle>Cancel order?</DialogTitle><DialogDescription>This will cancel <strong>{order.orderNumber}</strong>. The Order service will release associated reservations as part of the cancellation workflow.</DialogDescription></DialogHeader>{cancel.error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{errorMessage(cancel.error)}</div>}<div className="flex justify-end gap-2"><Button variant="outline" onClick={() => setOpen(false)} disabled={cancel.isPending}>Keep order</Button><Button variant="destructive" onClick={execute} disabled={cancel.isPending}>{cancel.isPending ? "Cancelling…" : "Cancel order"}</Button></div></DialogContent></Dialog></>;
}
