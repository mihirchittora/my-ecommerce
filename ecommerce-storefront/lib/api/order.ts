import { orderClient } from "@/lib/api/client";
import type { OrderDetail, OrderSummary, PageResponse, PaymentMethodOptionsResponse } from "@/lib/types";

export const orderApi = {
  listMine: (page = 0) => orderClient.request<PageResponse<OrderSummary>>(`/v1/orders/my?page=${page}&size=12&sort=createdAt,desc`),
  get: (id: string) => orderClient.request<OrderDetail>(`/v1/orders/${encodeURIComponent(id)}`),
  paymentMethods: (currency: string, amount: number, country: string) => orderClient.request<PaymentMethodOptionsResponse>(`/v1/orders/payment-methods?currency=${encodeURIComponent(currency)}&amount=${encodeURIComponent(amount.toFixed(2))}&country=${encodeURIComponent(country)}`),
  cancel: (id: string) => orderClient.request<OrderDetail>(`/v1/orders/${encodeURIComponent(id)}/cancel`, { method: "POST" }),
};
