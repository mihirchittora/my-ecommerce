import type { PageResponse } from "@/lib/types";

export const SHIPMENT_STATUSES = ["CREATED", "READY", "PACKED", "SHIPPED", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "DELIVERY_FAILED", "RETURNED", "CANCELLED", "FAILED"] as const;
export type ShipmentStatus = (typeof SHIPMENT_STATUSES)[number];

export const FULFILLMENT_STATUSES = ["PENDING", "READY", "ALLOCATING", "PACKED", "PARTIALLY_SHIPPED", "SHIPPED", "DELIVERED", "COMPLETED", "CANCELLED", "FAILED"] as const;
export type FulfillmentStatus = (typeof FULFILLMENT_STATUSES)[number];

export type ShipmentSort = "createdAt,desc" | "createdAt,asc" | "updatedAt,desc" | "updatedAt,asc" | "status,asc" | "status,desc" | "shipmentNumber,asc" | "shipmentNumber,desc" | "orderNumber,asc" | "orderNumber,desc";
export type FulfillmentSort = "createdAt,desc" | "createdAt,asc" | "updatedAt,desc" | "updatedAt,asc" | "status,asc" | "status,desc" | "orderNumber,asc" | "orderNumber,desc";

export interface ShipmentListParams {
  page: number;
  size: number;
  sort: ShipmentSort;
  search?: string;
  status?: ShipmentStatus;
  carrier?: string;
  createdFrom?: string;
  createdTo?: string;
}
export interface FulfillmentListParams { page: number; size: number; sort: FulfillmentSort; }

export interface ShipmentSummary {
  id: string;
  shipmentNumber: string;
  fulfillmentId: string;
  orderId: string;
  orderNumber: string;
  status: ShipmentStatus;
  carrier: string;
  serviceLevel: string;
  trackingNumber: string | null;
  currency: string;
  packageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ShipmentItem {
  id: string;
  shipmentId: string;
  orderItemId: string;
  sku: string;
  productNameSnapshot: string;
  quantity: number;
  inventoryUnitId: string | null;
  createdAt: string;
}

export interface ShipmentHistoryEntry {
  id: string;
  fromStatus: ShipmentStatus | null;
  toStatus: ShipmentStatus;
  eventType: string;
  carrierEventId: string | null;
  referenceId: string | null;
  notes: string | null;
  createdAt: string;
}

export interface ShipmentDetail extends ShipmentSummary {
  customerId: string;
  trackingNumber: string | null;
  providerShipmentId: string | null;
  labelReference: string | null;
  shippingCost: number;
  estimatedDeliveryAt: string | null;
  orderNotificationPending: boolean;
  shippedAt: string | null;
  deliveredAt: string | null;
  cancelledAt: string | null;
  items: ShipmentItem[];
  history: ShipmentHistoryEntry[];
}

export interface TrackingEvent {
  id: string;
  trackingNumber: string | null;
  carrier: string;
  eventType: string;
  eventStatus: string;
  eventLocation: string | null;
  description: string | null;
  occurredAt: string;
  receivedAt: string | null;
}

export interface TrackingResponse {
  shipmentNumber: string;
  carrier: string;
  trackingNumber: string | null;
  currentStatus: ShipmentStatus;
  estimatedDeliveryAt: string | null;
  events: TrackingEvent[];
}

export interface ShippingAddress {
  sourceAddressId: string | null;
  recipientName: string | null;
  phone: string | null;
  line1: string | null;
  line2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  landmark: string | null;
}

export interface FulfillmentItem {
  id: string;
  orderItemId: string;
  sku: string;
  productNameSnapshot: string;
  quantity: number;
  inventoryUnitIds: string[];
  inventoryUnitCodes: string[];
  createdAt: string;
}

export interface FulfillmentHistoryEntry {
  id: string;
  fromStatus: FulfillmentStatus | null;
  toStatus: FulfillmentStatus;
  eventType: string;
  referenceId: string | null;
  notes: string | null;
  createdAt: string;
}

export interface FulfillmentDetail {
  id: string;
  orderId: string;
  orderNumber: string;
  customerId: string;
  status: FulfillmentStatus;
  shippingAddressReference: string | null;
  shippingAddress: ShippingAddress | null;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
  cancelledAt: string | null;
  items: FulfillmentItem[];
  history: FulfillmentHistoryEntry[];
  shipments: ShipmentSummary[];
}

export type ShipmentPage = PageResponse<ShipmentSummary>;
export type FulfillmentPage = PageResponse<FulfillmentDetail>;

export interface CreateShipmentLine {
  orderItemId: string;
  quantity: number;
  inventoryUnitIds?: string[];
}

export interface CreateShipmentPayload {
  fulfillmentId: string;
  carrier: string;
  serviceLevel: string;
  shippingCost?: number;
  currency?: string;
  lines: CreateShipmentLine[];
}

export function isShipmentStatus(value: string): value is ShipmentStatus {
  return SHIPMENT_STATUSES.includes(value as ShipmentStatus);
}

export function isFulfillmentStatus(value: string): value is FulfillmentStatus {
  return FULFILLMENT_STATUSES.includes(value as FulfillmentStatus);
}
