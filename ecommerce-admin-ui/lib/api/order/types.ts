import type { PageResponse } from "@/lib/types";

export const ORDER_STATUSES = [
  "DRAFT",
  "PENDING_RESERVATION",
  "RESERVED",
  "PENDING_PAYMENT",
  "PAID",
  "CONFIRMED",
  "FULFILLING",
  "SHIPPED",
  "DELIVERED",
  "COMPLETED",
  "CANCELLED",
  "FAILED",
] as const;

export type OrderStatus = (typeof ORDER_STATUSES)[number];
export type PaymentMethod = "ONLINE" | "CASH_ON_DELIVERY";
export type OrderSort = "createdAt,desc" | "createdAt,asc" | "updatedAt,desc" | "updatedAt,asc" | "orderNumber,asc" | "orderNumber,desc" | "totalAmount,desc" | "totalAmount,asc" | "status,asc" | "status,desc";

export interface OrderListParams {
  page: number;
  size: number;
  sort: OrderSort;
  orderNumber?: string;
  status?: OrderStatus;
  sku?: string;
  createdFrom?: string;
  createdTo?: string;
}

export interface OrderSummary {
  id: string;
  orderNumber: string;
  customerId: string;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  currency: string;
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
}

export type VariantSnapshot = Record<string, unknown>;

export interface InventoryUnitReference {
  id: string;
  unitCode: string | null;
}

export interface OrderItem {
  id: string;
  orderId: string;
  sku: string;
  productNameSnapshot: string;
  variantSnapshot: VariantSnapshot;
  unitPrice: number;
  currency: string;
  quantity: number;
  subtotal: number;
  createdAt: string;
  reservationId: string | null;
  reservationReference: string | null;
  inventoryUnitIds: string[];
  inventoryUnits?: InventoryUnitReference[];
}

export interface OrderHistoryEntry {
  id: string;
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus;
  eventType: string;
  referenceId: string | null;
  notes: string | null;
  actorUserId: string | null;
  createdAt: string;
}

export interface OrderDetail {
  id: string;
  orderNumber: string;
  customerId: string;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  currency: string;
  subtotal: number;
  discountAmount: number;
  shippingAmount: number;
  taxAmount: number;
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
  cancelledAt: string | null;
  completedAt: string | null;
  items: OrderItem[];
  history: OrderHistoryEntry[];
}

export type OrderPage = PageResponse<OrderSummary>;
