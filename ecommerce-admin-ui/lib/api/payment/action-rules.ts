import type { Payment, PaymentStatus } from "@/lib/api/payment/types";

export type PaymentAction = "refund" | "retry" | "collect";

export function canRefundPayment(status: PaymentStatus, permissions: readonly string[]) {
  return permissions.includes("PAYMENT_REFUND") && (status === "CAPTURED" || status === "PARTIALLY_REFUNDED");
}

export function canRetryPayment(status: PaymentStatus, permissions: readonly string[]) {
  return permissions.includes("PAYMENT_RETRY") && status === "FAILED";
}

export function canCollectCod(payment: Pick<Payment, "status" | "paymentMethod">, permissions: readonly string[]) {
  return permissions.includes("PAYMENT_COLLECT") && payment.paymentMethod === "CASH_ON_DELIVERY" && payment.status === "PENDING_COLLECTION";
}

export function getAllowedPaymentActions(payment: Pick<Payment, "status" | "paymentMethod">, permissions: readonly string[]): PaymentAction[] {
  return [
    ...(canRefundPayment(payment.status, permissions) ? ["refund" as const] : []),
    ...(canRetryPayment(payment.status, permissions) ? ["retry" as const] : []),
    ...(canCollectCod(payment, permissions) ? ["collect" as const] : []),
  ];
}
