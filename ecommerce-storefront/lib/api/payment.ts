import { paymentClient } from "@/lib/api/client";
import type { Payment, PaymentMethod } from "@/lib/types";

export const paymentApi = {
  forOrder: (orderId: string) => paymentClient.request<Payment[]>(`/v1/payments/order/${encodeURIComponent(orderId)}`),
  create: (orderId: string, paymentMethod: PaymentMethod, idempotencyKey: string) => paymentClient.json<Payment, { orderId: string; paymentMethod: PaymentMethod }>("/v1/payments", { orderId, paymentMethod }, { headers: { "Idempotency-Key": idempotencyKey } }),
  retry: (paymentId: string, idempotencyKey: string) => paymentClient.request<Payment>(`/v1/payments/${encodeURIComponent(paymentId)}/retry`, { method: "POST", headers: { "Idempotency-Key": idempotencyKey } }),
};
