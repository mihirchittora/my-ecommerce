"use client";

import { useMemo, useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { useToast } from "@/components/ui/toast";
import { useCreateShipment } from "@/lib/api/shipping/queries";
import { canCreateShipment } from "@/lib/api/shipping/action-rules";
import type { FulfillmentDetail } from "@/lib/api/shipping/types";
import { ApiError } from "@/lib/api/client";

function initialQuantities(fulfillment: FulfillmentDetail) {
  return Object.fromEntries(fulfillment.items.map((item) => [item.orderItemId, item.quantity]));
}

function initialUnits(fulfillment: FulfillmentDetail) {
  return Object.fromEntries(fulfillment.items.map((item) => [item.orderItemId, item.inventoryUnitIds]));
}

export function CreateShipmentDialog({ fulfillment }: { fulfillment: FulfillmentDetail }) {
  const { user } = useAuth();
  const { toast } = useToast();
  const create = useCreateShipment();
  const [open, setOpen] = useState(false);
  const [carrier, setCarrier] = useState("SANDBOX");
  const [serviceLevel, setServiceLevel] = useState("STANDARD");
  const [shippingCost, setShippingCost] = useState("");
  const [quantities, setQuantities] = useState<Record<string, number>>(() => initialQuantities(fulfillment));
  const [units, setUnits] = useState<Record<string, string[]>>(() => initialUnits(fulfillment));
  const [validationError, setValidationError] = useState<string | null>(null);
  const allowed = canCreateShipment(fulfillment.status, user?.permissions ?? []);
  const hasItems = useMemo(() => fulfillment.items.some((item) => (item.inventoryUnitIds.length ? (units[item.orderItemId] ?? []).length > 0 : (quantities[item.orderItemId] ?? 0) > 0)), [fulfillment.items, quantities, units]);
  if (!allowed) return null;

  const toggleUnit = (orderItemId: string, unitId: string) => setUnits((current) => { const selected = current[orderItemId] ?? []; return { ...current, [orderItemId]: selected.includes(unitId) ? selected.filter((value) => value !== unitId) : [...selected, unitId] }; });
  const submit = () => {
    setValidationError(null);
    const lines = fulfillment.items.flatMap((item) => {
      if (item.inventoryUnitIds.length) {
        const selected = units[item.orderItemId] ?? [];
        return selected.length ? [{ orderItemId: item.orderItemId, quantity: selected.length, inventoryUnitIds: selected }] : [];
      }
      const quantity = quantities[item.orderItemId] ?? 0;
      return quantity > 0 ? [{ orderItemId: item.orderItemId, quantity }] : [];
    });
    if (!lines.length) { setValidationError("Select at least one shipment item."); return; }
    const cost = shippingCost.trim() ? Number(shippingCost) : undefined;
    if (cost !== undefined && (!Number.isFinite(cost) || cost < 0)) { setValidationError("Shipping cost must be a non-negative number."); return; }
    create.mutate({ payload: { fulfillmentId: fulfillment.id, carrier: carrier.trim().toUpperCase(), serviceLevel: serviceLevel.trim().toUpperCase(), shippingCost: cost, lines }, idempotencyKey: undefined }, { onSuccess: (shipment) => { setOpen(false); toast({ title: "Shipment created", description: `${shipment.shipmentNumber} is ready for tracking.` }); }, onError: (error) => setValidationError(error instanceof ApiError ? error.message : "Shipment creation failed. Please try again.") });
  };

  return <><Button onClick={() => setOpen(true)}>Create shipment</Button><Dialog open={open} onOpenChange={(nextOpen) => { if (!create.isPending) setOpen(nextOpen); }}><DialogContent className="max-w-2xl"><DialogHeader><DialogTitle>Create shipment</DialogTitle><DialogDescription>Create a carrier shipment from the selected fulfillment references. Inventory Units remain owned by Inventory Service.</DialogDescription></DialogHeader><div className="space-y-5"><div className="grid gap-4 sm:grid-cols-3"><div><Label htmlFor="carrier">Carrier</Label><Select id="carrier" className="mt-2 w-full" value={carrier} onChange={(event) => setCarrier(event.target.value)}><option value="SANDBOX">SANDBOX</option><option value="EASYPOST">EASYPOST</option></Select><p className="mt-1 text-xs leading-5 text-slate-500">Must match the provider configured in Shipping Service.</p></div><div><Label htmlFor="serviceLevel">Service level</Label><Input id="serviceLevel" className="mt-2" value={serviceLevel} onChange={(event) => setServiceLevel(event.target.value)} required maxLength={50} /></div><div><Label htmlFor="shippingCost">Shipping cost</Label><Input id="shippingCost" className="mt-2" type="number" min="0" step="0.01" value={shippingCost} onChange={(event) => setShippingCost(event.target.value)} placeholder="Optional" /></div></div><div><p className="text-sm font-semibold text-slate-800">Shipment lines</p><p className="mt-1 text-xs leading-5 text-slate-500">Select individual physical units when Shipping returned them; otherwise choose an aggregate quantity.</p><div className="mt-3 space-y-3">{fulfillment.items.map((item) => <div className="rounded-2xl border border-slate-100 p-4" key={item.orderItemId}><div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between"><div><p className="text-sm font-semibold text-slate-800">{item.productNameSnapshot}</p><p className="mt-1 font-mono text-xs text-slate-500">{item.sku} · Order item {item.orderItemId}</p></div>{item.inventoryUnitIds.length === 0 && <label className="text-sm text-slate-600">Quantity<input className="ml-2 h-9 w-20 rounded-lg border border-input px-2 text-right" type="number" min="0" max={item.quantity} value={quantities[item.orderItemId] ?? 0} onChange={(event) => setQuantities((current) => ({ ...current, [item.orderItemId]: Math.min(item.quantity, Math.max(0, Number(event.target.value) || 0)) }))} /></label>}</div>{item.inventoryUnitIds.length > 0 && <div className="mt-3 grid gap-2 sm:grid-cols-2">{item.inventoryUnitIds.map((unitId, index) => <label className="flex cursor-pointer items-center gap-2 rounded-xl bg-slate-50 px-3 py-2 text-xs" key={unitId}><input type="checkbox" checked={(units[item.orderItemId] ?? []).includes(unitId)} onChange={() => toggleUnit(item.orderItemId, unitId)} /><span className="font-mono font-semibold">{item.inventoryUnitCodes[index] ?? unitId}</span><span className="truncate text-slate-400">{unitId}</span></label>)}</div>}</div>)}</div></div>{validationError && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{validationError}</div>}<div className="flex justify-end gap-2"><Button variant="outline" onClick={() => setOpen(false)} disabled={create.isPending}>Cancel</Button><Button onClick={submit} disabled={create.isPending || !hasItems}>{create.isPending ? "Creating…" : "Create shipment"}</Button></div></div></DialogContent></Dialog></>;
}
