import { paymentClient } from "@/lib/api/payment/client";
import type { Payment, PaymentListParams, PaymentPage, PaymentSort, RefundPayload } from "@/lib/api/payment/types";

function query(values: Record<string, string | number | undefined>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== "") params.set(key, String(value));
  });
  const text = params.toString();
  return text ? `?${text}` : "";
}

function dayStart(value: string | undefined) {
  return value ? `${value}T00:00:00Z` : undefined;
}

function dayEndExclusive(value: string | undefined) {
  if (!value) return undefined;
  const nextDay = new Date(`${value}T00:00:00Z`);
  nextDay.setUTCDate(nextDay.getUTCDate() + 1);
  return nextDay.toISOString();
}

export const paymentApi = {
  list: (params: PaymentListParams) => paymentClient.request<PaymentPage>(`/v1/admin/payments${query({
    page: params.page,
    size: params.size,
    sort: params.sort,
    search: params.search,
    status: params.status,
    provider: params.provider,
    currency: params.currency,
    orderId: params.orderId,
    customerId: params.customerId,
    createdFrom: dayStart(params.createdFrom),
    createdTo: dayEndExclusive(params.createdTo),
  })}`),
  get: (id: string) => paymentClient.request<Payment>(`/v1/admin/payments/${encodeURIComponent(id)}`),
  refund: (id: string, payload: RefundPayload, idempotencyKey: string) => paymentClient.json<Payment, RefundPayload>(
    `/v1/admin/payments/${encodeURIComponent(id)}/refunds`,
    payload,
    { method: "POST", headers: { "Idempotency-Key": idempotencyKey } },
  ),
  retry: (id: string, idempotencyKey: string) => paymentClient.request<Payment>(
    `/v1/admin/payments/${encodeURIComponent(id)}/retry`,
    { method: "POST", headers: { "Idempotency-Key": idempotencyKey } },
  ),
};

export function newPaymentIdempotencyKey(operation: "refund" | "retry") {
  const random = typeof crypto !== "undefined" && "randomUUID" in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  return `admin-${operation}-${random}`;
}

export function isPaymentSort(value: string): value is PaymentSort {
  return [
    "createdAt,desc", "createdAt,asc", "updatedAt,desc", "updatedAt,asc",
    "amount,desc", "amount,asc", "status,asc", "status,desc",
    "provider,asc", "provider,desc", "refundedAmount,desc", "refundedAmount,asc",
  ].includes(value as PaymentSort);
}
