"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useParams } from "next/navigation";
import { useAuth } from "@/components/auth-provider";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { CreateShipmentDialog } from "@/components/shipping/create-shipment-dialog";
import { FulfillmentCustomerPanel, FulfillmentHistoryTimeline, FulfillmentItems, FulfillmentOverview, FulfillmentShipments } from "@/components/shipping/fulfillment-detail-sections";
import { FulfillmentStatusBadge } from "@/components/shipping/status-badges";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/client";
import { useCustomerByAuthUserId } from "@/lib/api/customer/queries";
import { useOrder } from "@/lib/api/order/queries";
import { useFulfillment } from "@/lib/api/shipping/queries";
import { formatDate } from "@/lib/utils";

function errorState(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 403) return { title: "Fulfillment access denied", message: "You do not have permission to view fulfillments." };
    if (error.status === 404) return { title: "Fulfillment not found", message: "The fulfillment may have been removed or the link may be invalid." };
    if (error.status === 0 || error.status >= 500) return { title: "Shipping Service unavailable", message: "Shipping Service is temporarily unavailable." };
    return { title: "Unable to load fulfillment", message: error.message };
  }
  return { title: "Unable to load fulfillment", message: "Shipping Service is temporarily unavailable." };
}

function dependencyMessage(error: unknown, name: string) {
  if (error instanceof ApiError && error.status === 403) return `You do not have permission to view ${name.toLowerCase()} details.`;
  if (error instanceof ApiError && error.status === 404) return `${name} information is unavailable for this reference.`;
  return `${name} information is temporarily unavailable.`;
}

export default function FulfillmentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { hasPermission } = useAuth();
  const fulfillment = useFulfillment(id);
  const canReadOrder = hasPermission("ORDER_READ");
  const canReadCustomer = hasPermission("CUSTOMER_READ");
  const canReadInventory = hasPermission("INVENTORY_READ") || hasPermission("INVENTORY_UNIT_READ");
  const order = useOrder(fulfillment.data?.orderId ?? "", canReadOrder && Boolean(fulfillment.data));
  const customer = useCustomerByAuthUserId(fulfillment.data?.customerId ?? "", canReadCustomer && Boolean(fulfillment.data));
  if (fulfillment.isLoading) return <LoadingCard rows={8} />;
  if (fulfillment.isError || !fulfillment.data) { const error = errorState(fulfillment.error); return <ErrorState title={error.title} message={error.message} onRetry={() => void fulfillment.refetch()} />; }
  const target = fulfillment.data;
  return <><div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/fulfillments"><ArrowLeft className="h-4 w-4" />Back to fulfillments</Link></Button></div><div className="mb-8 flex flex-col gap-4 md:flex-row md:items-end md:justify-between"><div><p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">Fulfillment detail</p><div className="flex flex-wrap items-center gap-3"><h1 className="font-mono text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">{target.id}</h1><FulfillmentStatusBadge status={target.status} /></div><p className="mt-2 text-sm text-slate-500">Order {target.orderNumber} · Created {formatDate(target.createdAt)}</p></div><CreateShipmentDialog fulfillment={target} /></div><div className="space-y-6"><FulfillmentOverview fulfillment={target} customer={customer.data} canReadOrder={canReadOrder} canReadCustomer={canReadCustomer} /><FulfillmentCustomerPanel customer={customer.data} customerId={target.customerId} canReadCustomer={canReadCustomer} errorMessage={canReadCustomer ? customer.isError ? dependencyMessage(customer.error, "Customer") : undefined : "Customer details require CUSTOMER_READ permission."} /><FulfillmentItems fulfillment={target} canReadInventory={canReadInventory} /><FulfillmentShipments fulfillment={target} /><FulfillmentHistoryTimeline entries={target.history} />{canReadOrder && <div className="rounded-2xl border border-blue-100 bg-blue-50/70 px-4 py-3 text-sm leading-6 text-blue-900">Current Order status: {order.isError ? "Order information is temporarily unavailable." : order.isLoading ? "Loading…" : order.data?.status ?? "Not returned"}. Shipping actions do not directly change Order status.</div>}</div></>;
}
