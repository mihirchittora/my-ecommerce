import { ArrowDownAZ } from "lucide-react";
import { Select } from "@/components/ui/select";
import type { FulfillmentSort, ShipmentSort } from "@/lib/api/shipping/types";

export function ShippingSortControl({ value, onChange, kind }: { value: ShipmentSort | FulfillmentSort; onChange: (value: ShipmentSort | FulfillmentSort) => void; kind: "shipment" | "fulfillment" }) {
  const options = kind === "shipment" ? ["createdAt,desc", "createdAt,asc", "updatedAt,desc", "updatedAt,asc", "status,asc", "status,desc", "shipmentNumber,asc", "shipmentNumber,desc", "orderNumber,asc", "orderNumber,desc"] : ["createdAt,desc", "createdAt,asc", "updatedAt,desc", "updatedAt,asc", "status,asc", "status,desc", "orderNumber,asc", "orderNumber,desc"];
  return <label className="flex items-center gap-2 text-sm text-slate-500"><ArrowDownAZ className="h-4 w-4" /><span className="sr-only">Sort</span><Select value={value} onChange={(event) => onChange(event.target.value as ShipmentSort | FulfillmentSort)} className="w-auto min-w-44"><option value={options[0]}>Newest first</option>{options.slice(1).map((option) => <option value={option} key={option}>{option.replace(",", " · ")}</option>)}</Select></label>;
}
