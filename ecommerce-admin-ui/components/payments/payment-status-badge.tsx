import { Badge } from "@/components/ui/badge";
import type { PaymentAttemptStatus, PaymentStatus, RefundStatus } from "@/lib/api/payment/types";
import { titleCase } from "@/lib/utils";

const paymentVariants: Record<PaymentStatus, "default" | "success" | "warning" | "danger" | "muted"> = {
  CREATED: "muted",
  PENDING: "warning",
  AUTHORIZED: "default",
  CAPTURED: "success",
  PENDING_COLLECTION: "warning",
  FAILED: "danger",
  CANCELLED: "muted",
  REFUND_PENDING: "warning",
  PARTIALLY_REFUNDED: "warning",
  REFUNDED: "success",
};

const refundVariants: Record<RefundStatus, "default" | "success" | "warning" | "danger" | "muted"> = {
  PENDING: "warning",
  SUCCEEDED: "success",
  FAILED: "danger",
};

const attemptVariants: Record<PaymentAttemptStatus, "default" | "success" | "warning" | "danger" | "muted"> = {
  CREATED: "muted",
  PENDING: "warning",
  AUTHORIZED: "default",
  CAPTURED: "success",
  FAILED: "danger",
  CANCELLED: "muted",
};

export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  return <Badge variant={paymentVariants[status]} aria-label={`Payment status: ${titleCase(status)}`}>{titleCase(status)}</Badge>;
}

export function PaymentAttemptStatusBadge({ status }: { status: PaymentAttemptStatus }) {
  return <Badge variant={attemptVariants[status]} aria-label={`Attempt status: ${titleCase(status)}`}>{titleCase(status)}</Badge>;
}

export function RefundStatusBadge({ status }: { status: RefundStatus }) {
  return <Badge variant={refundVariants[status]} aria-label={`Refund status: ${titleCase(status)}`}>{titleCase(status)}</Badge>;
}
