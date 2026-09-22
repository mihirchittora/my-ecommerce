"use client";

import Link from "next/link";
import { ArrowLeft, Check, Circle, MapPin, Truck } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { shippingApi } from "@/lib/api/shipping";
import { orderApi } from "@/lib/api/order";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ErrorState, LoadingBlock, EmptyState } from "@/components/feedback";
import { RequireAuth } from "@/components/require-auth";
import { formatDate } from "@/lib/utils";
import type { ShipmentStatus } from "@/lib/types";

const progress: ShipmentStatus[] = ["CREATED", "PACKED", "SHIPPED", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED"];

function TrackingContents({ orderId }: { orderId: string }) {
  const order = useQuery({ queryKey: ["order", orderId], queryFn: () => orderApi.get(orderId) });
  const shipments = useQuery({ queryKey: ["shipments", "mine"], queryFn: shippingApi.listMine });
  const shipment = shipments.data?.content.find((item) => item.orderId === orderId);
  const tracking = useQuery({ queryKey: ["tracking", shipment?.id], queryFn: () => shippingApi.tracking(shipment?.id ?? ""), enabled: Boolean(shipment?.id) });
  if (order.isLoading || shipments.isLoading) return <LoadingBlock label="Loading tracking" />;
  if (order.isError || shipments.isError) return <ErrorState message="Tracking information is temporarily unavailable." retry={() => { void order.refetch(); void shipments.refetch(); }} />;
  return <div><Link href={`/orders/${encodeURIComponent(orderId)}`} className="inline-flex items-center gap-2 text-sm font-bold text-moss hover:text-ink"><ArrowLeft className="h-4 w-4" />Back to order</Link><div className="mt-8"><p className="eyebrow">Shipment tracking</p><h1 className="mt-3 font-display text-4xl font-bold tracking-tight md:text-6xl">{order.data?.orderNumber ?? "Your shipment"}</h1><p className="mt-4 text-sm text-ink/55">Follow the latest carrier updates for this order.</p></div>{!shipment ? <div className="mt-10"><EmptyState title="No tracking information available yet" message="Shipment details will appear here after your order is prepared." /></div> : tracking.isError ? <div className="mt-10"><ErrorState message="Tracking information is temporarily unavailable." retry={() => void tracking.refetch()} /></div> : tracking.isLoading ? <div className="mt-10"><LoadingBlock label="Loading carrier updates" /></div> : tracking.data ? <div className="mt-10 grid gap-6 lg:grid-cols-[1fr_340px]"><Card className="p-6 md:p-8"><div className="flex items-start justify-between gap-4"><div><p className="eyebrow">{tracking.data.carrier}</p><h2 className="mt-2 font-display text-2xl font-bold">{tracking.data.trackingNumber ?? "Tracking number pending"}</h2></div><span className="grid h-11 w-11 place-items-center rounded-2xl bg-sage text-moss"><Truck className="h-5 w-5" /></span></div><div className="mt-10 grid gap-7">{progress.map((status, index) => { const currentIndex = progress.indexOf(tracking.data?.currentStatus ?? "CREATED"); const complete = index <= currentIndex; const event = tracking.data?.events.find((item) => item.eventType === status || item.eventStatus === status); return <div key={status} className="relative flex gap-4">{index < progress.length - 1 ? <span className={`absolute left-[11px] top-7 h-10 w-px ${complete && index < currentIndex ? "bg-moss" : "bg-ink/10"}`} /> : null}<span className={`relative grid h-6 w-6 shrink-0 place-items-center rounded-full ${complete ? "bg-moss text-white" : "border border-ink/15 bg-white text-ink/25"}`}>{complete ? <Check className="h-3.5 w-3.5" /> : <Circle className="h-3 w-3" />}</span><div><p className={`text-sm font-bold ${complete ? "text-ink" : "text-ink/40"}`}>{status.replaceAll("_", " ")}</p><p className="mt-1 text-xs text-ink/50">{event?.description ?? (complete ? "Confirmed by the latest shipment status." : "Not reached yet")}{event?.occurredAt ? ` · ${formatDate(event.occurredAt, true)}` : ""}</p>{event?.eventLocation ? <p className="mt-1 flex items-center gap-1 text-xs text-ink/45"><MapPin className="h-3 w-3" />{event.eventLocation}</p> : null}</div></div>; })}</div></Card><Card className="h-fit p-6"><p className="eyebrow">Shipment</p><div className="mt-5 grid gap-4 text-sm"><div><p className="text-ink/50">Shipment number</p><p className="mt-1 font-semibold">{tracking.data.shipmentNumber}</p></div><div><p className="text-ink/50">Status</p><div className="mt-1"><Badge tone="success">{tracking.data.currentStatus.replaceAll("_", " ")}</Badge></div></div>{tracking.data.estimatedDeliveryAt ? <div><p className="text-ink/50">Estimated delivery</p><p className="mt-1 font-semibold">{formatDate(tracking.data.estimatedDeliveryAt)}</p></div> : null}</div><p className="mt-6 text-xs leading-5 text-ink/45">Carrier updates are supplied by Shipping Service and may take time to refresh.</p></Card></div> : null}</div>;
}

export default function TrackingPage({ orderId }: { orderId: string }) { return <div className="page-shell py-10 md:py-16"><RequireAuth><TrackingContents orderId={orderId} /></RequireAuth></div>; }
