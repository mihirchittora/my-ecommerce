import { Badge } from "@/components/ui/badge";
import type { OrderStatus } from "@/lib/api/order/types";
import { titleCase } from "@/lib/utils";

const variants: Record<OrderStatus, "default" | "success" | "warning" | "danger" | "muted"> = {
  DRAFT: "muted",
  PENDING_RESERVATION: "warning",
  RESERVED: "success",
  PENDING_PAYMENT: "warning",
  PAID: "success",
  CONFIRMED: "success",
  FULFILLING: "default",
  SHIPPED: "default",
  DELIVERED: "success",
  COMPLETED: "success",
  CANCELLED: "danger",
  FAILED: "danger",
};

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return <Badge variant={variants[status]} aria-label={`Order status: ${titleCase(status)}`}>{titleCase(status)}</Badge>;
}
