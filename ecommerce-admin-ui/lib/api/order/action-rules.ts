import type { OrderStatus } from "@/lib/api/order/types";

export const cancellableOrderStatuses: readonly OrderStatus[] = ["DRAFT", "PENDING_RESERVATION", "RESERVED", "PENDING_PAYMENT"];

export type OrderAction = "cancel";

export function canCancelOrder(status: OrderStatus, permissions: readonly string[]) {
  return permissions.includes("ORDER_CANCEL") && cancellableOrderStatuses.includes(status);
}

export function getAllowedOrderActions(order: { status: OrderStatus }, permissions: readonly string[]): OrderAction[] {
  return canCancelOrder(order.status, permissions) ? ["cancel"] : [];
}
