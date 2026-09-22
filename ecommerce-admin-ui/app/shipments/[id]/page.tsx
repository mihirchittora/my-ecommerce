"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useParams } from "next/navigation";
import { useAuth } from "@/components/auth-provider";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { ShipmentActions } from "@/components/shipping/shipment-actions";
import { ShipmentCustomerPanel, ShipmentFulfillmentContext, ShipmentHistoryTimeline, ShipmentItems, ShipmentOrderContext, ShipmentOverview, ShipmentTrackingPanel } from "@/components/shipping/shipment-detail-sections";
import { ShipmentStatusBadge } from "@/components/shipping/status-badges";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/client";
import { useCustomerByAuthUserId } from "@/lib/api/customer/queries";
import { useOrder } from "@/lib/api/order/queries";
import { useFulfillment, useShipment, useShipmentTracking } from "@/lib/api/shipping/queries";
import { formatDate } from "@/lib/utils";

function errorState(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 403) return { title: "Shipment access denied", message: "You do not have permission to view shipments." };
    if (error.status === 404) return { title: "Shipment not found", message: "The shipment may have been removed or the link may be invalid." };
    if (error.status === 0 || error.status >= 500) return { title: "Shipping Service unavailable", message: "Shipping Service is temporarily unavailable." };
    return { title: "Unable to load shipment", message: error.message };
  }
  return { title: "Unable to load shipment", message: "Shipping Service is temporarily unavailable." };
}

function dependencyMessage(error: unknown, name: string) {
  if (error instanceof ApiError && error.status === 403) return `You do not have permission to view ${name.toLowerCase()} details.`;
  if (error instanceof ApiError && error.status === 404) return `${name} information is unavailable for this reference.`;
  return `${name} information is temporarily unavailable.`;
}

export default function ShipmentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { hasPermission } = useAuth();
  const shipment = useShipment(id);
  const canReadOrder = hasPermission("ORDER_READ");
  const canReadCustomer = hasPermission("CUSTOMER_READ");
  const canReadInventory = hasPermission("INVENTORY_READ") || hasPermission("INVENTORY_UNIT_READ");
  const canTrack = hasPermission("SHIPPING_TRACK");
  const order = useOrder(shipment.data?.orderId ?? "", canReadOrder && Boolean(shipment.data));
  const customer = useCustomerByAuthUserId(shipment.data?.customerId ?? "", canReadCustomer && Boolean(shipment.data));
  const fulfillment = useFulfillment(shipment.data?.fulfillmentId ?? "", Boolean(shipment.data));
  const tracking = useShipmentTracking(id, canTrack && Boolean(shipment.data));

  if (shipment.isLoading) return <LoadingCard rows={8} />;
  if (shipment.isError || !shipment.data) { const error = errorState(shipment.error); return <ErrorState title={error.title} message={error.message} onRetry={() => void shipment.refetch()} />; }
  const target = shipment.data;
  return <><div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/shipments"><ArrowLeft className="h-4 w-4" />Back to shipments</Link></Button></div><div className="mb-8 flex flex-col gap-4 md:flex-row md:items-end md:justify-between"><div><p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">Shipment detail</p><div className="flex flex-wrap items-center gap-3"><h1 className="font-mono text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">{target.shipmentNumber}</h1><ShipmentStatusBadge status={target.status} /></div><p className="mt-2 text-sm text-slate-500">{target.carrier} · Created {formatDate(target.createdAt)}</p></div><ShipmentActions shipment={target} /></div><div className="space-y-6"><ShipmentOverview shipment={target} canReadOrder={canReadOrder} /><ShipmentOrderContext orderId={target.orderId} orderNumber={target.orderNumber} status={order.data?.status} canReadOrder={canReadOrder} errorMessage={canReadOrder ? order.isError ? dependencyMessage(order.error, "Order") : undefined : "Order details require ORDER_READ permission."} /><ShipmentCustomerPanel customer={customer.data} customerId={target.customerId} canReadCustomer={canReadCustomer} errorMessage={canReadCustomer ? customer.isError ? dependencyMessage(customer.error, "Customer") : undefined : "Customer details require CUSTOMER_READ permission."} /><ShipmentFulfillmentContext fulfillmentId={target.fulfillmentId} status={fulfillment.data?.status} errorMessage={fulfillment.isError ? dependencyMessage(fulfillment.error, "Fulfillment") : undefined} /><ShipmentItems shipment={target} canReadInventory={canReadInventory} />{canTrack ? <ShipmentTrackingPanel tracking={tracking.data} isLoading={tracking.isLoading} errorMessage={tracking.isError ? dependencyMessage(tracking.error, "Tracking") : undefined} onRetry={() => void tracking.refetch()} /> : <ShipmentTrackingPanel tracking={undefined} isLoading={false} errorMessage="Tracking requires SHIPPING_TRACK permission." />}<ShipmentHistoryTimeline entries={target.history} /></div></>;
}
