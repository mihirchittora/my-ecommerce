import { Badge } from "@/components/ui/badge";
import type { FulfillmentStatus, ShipmentStatus } from "@/lib/api/shipping/types";
import { titleCase } from "@/lib/utils";

const shipmentVariants: Record<ShipmentStatus, "default" | "success" | "warning" | "danger" | "muted"> = {
  CREATED: "muted", READY: "default", PACKED: "default", SHIPPED: "default", IN_TRANSIT: "default", OUT_FOR_DELIVERY: "warning", DELIVERED: "success", DELIVERY_FAILED: "danger", RETURNED: "warning", CANCELLED: "danger", FAILED: "danger",
};

const fulfillmentVariants: Record<FulfillmentStatus, "default" | "success" | "warning" | "danger" | "muted"> = {
  PENDING: "muted", READY: "default", ALLOCATING: "warning", PACKED: "default", PARTIALLY_SHIPPED: "warning", SHIPPED: "default", DELIVERED: "success", COMPLETED: "success", CANCELLED: "danger", FAILED: "danger",
};

export function ShipmentStatusBadge({ status }: { status: ShipmentStatus }) {
  return <Badge variant={shipmentVariants[status]} aria-label={`Shipment status: ${titleCase(status)}`}>{titleCase(status)}</Badge>;
}

export function FulfillmentStatusBadge({ status }: { status: FulfillmentStatus }) {
  return <Badge variant={fulfillmentVariants[status]} aria-label={`Fulfillment status: ${titleCase(status)}`}>{titleCase(status)}</Badge>;
}
