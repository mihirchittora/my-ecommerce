"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useToast } from "@/components/ui/toast";
import { useAuth } from "@/components/auth-provider";
import { useCancelShipment, useDeliverShipment } from "@/lib/api/shipping/queries";
import { canCancelShipment, canDeliverShipment } from "@/lib/api/shipping/action-rules";
import type { ShipmentDetail, ShipmentSummary } from "@/lib/api/shipping/types";
import { ApiError } from "@/lib/api/client";

type ShipmentActionTarget = ShipmentSummary | ShipmentDetail;

function errorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 403) return "You do not have permission to manage shipments.";
    if (error.status === 404) return "Shipment was not found or is no longer available.";
    if (error.status === 409) return "Shipment cannot be cancelled in its current state.";
    if (error.status === 502 || error.status === 503) return "Carrier service is unavailable; the shipment was not cancelled.";
    return error.message;
  }
  return "The shipment could not be cancelled. Please try again.";
}

export function ShipmentActions({ shipment }: { shipment: ShipmentActionTarget }) {
  const { user } = useAuth();
  const permissions = user?.permissions ?? [];
  const canCancel = canCancelShipment(shipment.status, permissions);
  const canDeliver = canDeliverShipment(shipment.status, permissions);
  if (!canCancel && !canDeliver) return null;
  return <div className="flex flex-wrap gap-2">{canDeliver && <DeliverShipmentDialog shipment={shipment} />}{canCancel && <CancelShipmentDialog shipment={shipment} />}</div>;
}

export function DeliverShipmentDialog({ shipment }: { shipment: ShipmentActionTarget }) {
  const [open, setOpen] = useState(false);
  const deliver = useDeliverShipment();
  const { toast } = useToast();
  const execute = () => deliver.mutate({ shipmentId: shipment.id }, { onSuccess: () => { setOpen(false); toast({ title: "Shipment marked delivered", description: `${shipment.shipmentNumber} was marked delivered.` }); } });
  return <><Button variant="outline" size="sm" onClick={() => setOpen(true)}>Mark delivered</Button><Dialog open={open} onOpenChange={(nextOpen) => { if (!deliver.isPending) setOpen(nextOpen); }}><DialogContent><DialogHeader><DialogTitle>Mark shipment delivered?</DialogTitle><DialogDescription>This records a manual delivery event for <strong>{shipment.shipmentNumber}</strong> and notifies the Order and Fulfillment services.</DialogDescription></DialogHeader>{deliver.error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{errorMessage(deliver.error)}</div>}<div className="flex justify-end gap-2"><Button variant="outline" onClick={() => setOpen(false)} disabled={deliver.isPending}>Keep shipment</Button><Button onClick={execute} disabled={deliver.isPending}>{deliver.isPending ? "Updating…" : "Mark delivered"}</Button></div></DialogContent></Dialog></>;
}

export function CancelShipmentDialog({ shipment }: { shipment: ShipmentActionTarget }) {
  const [open, setOpen] = useState(false);
  const cancel = useCancelShipment();
  const { toast } = useToast();
  const execute = () => cancel.mutate({ shipmentId: shipment.id }, { onSuccess: () => { setOpen(false); toast({ title: "Shipment cancelled", description: `${shipment.shipmentNumber} was cancelled.` }); } });
  return <><Button variant="outline" size="sm" className="text-rose-600 hover:text-rose-700" onClick={() => setOpen(true)}>Cancel shipment</Button><Dialog open={open} onOpenChange={(nextOpen) => { if (!cancel.isPending) setOpen(nextOpen); }}><DialogContent><DialogHeader><DialogTitle>Cancel shipment?</DialogTitle><DialogDescription>This may prevent further carrier processing for <strong>{shipment.shipmentNumber}</strong>. The Shipping Service only permits cancellation before carrier handoff.</DialogDescription></DialogHeader>{cancel.error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{errorMessage(cancel.error)}</div>}<div className="flex justify-end gap-2"><Button variant="outline" onClick={() => setOpen(false)} disabled={cancel.isPending}>Keep shipment</Button><Button variant="destructive" onClick={execute} disabled={cancel.isPending}>{cancel.isPending ? "Cancelling…" : "Cancel shipment"}</Button></div></DialogContent></Dialog></>;
}
