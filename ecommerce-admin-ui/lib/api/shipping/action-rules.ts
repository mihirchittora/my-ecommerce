import type { FulfillmentStatus, ShipmentStatus } from "@/lib/api/shipping/types";

const cancellableStatuses: ShipmentStatus[] = ["CREATED", "READY", "PACKED", "FAILED"];
const shipmentReadyFulfillmentStatuses: FulfillmentStatus[] = ["READY", "ALLOCATING", "PACKED", "PARTIALLY_SHIPPED"];

export function canCancelShipment(status: ShipmentStatus, permissions: string[]) {
  return permissions.includes("SHIPPING_CANCEL") && cancellableStatuses.includes(status);
}

export function canCreateShipment(status: FulfillmentStatus, permissions: string[]) {
  return permissions.includes("SHIPPING_CREATE") && shipmentReadyFulfillmentStatuses.includes(status);
}
