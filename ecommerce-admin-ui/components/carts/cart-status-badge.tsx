import { Badge } from "@/components/ui/badge";
import type { CartStatus } from "@/lib/api/cart/types";
import { titleCase } from "@/lib/utils";

const variants: Record<CartStatus, "default" | "success" | "warning" | "danger" | "muted"> = {
  ACTIVE: "success",
  CHECKOUT_IN_PROGRESS: "warning",
  CONVERTED: "default",
  ABANDONED: "muted",
  EXPIRED: "danger",
};

export function CartStatusBadge({ status }: { status: CartStatus }) {
  return <Badge variant={variants[status]} aria-label={`Cart status: ${titleCase(status)}`}>{titleCase(status)}</Badge>;
}
