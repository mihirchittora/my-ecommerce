import { orderClient } from "@/lib/api/client";
import type { CouponPreviewResponse, OrderDetail, OrderSummary, PageResponse, PaymentMethodOptionsResponse, ShippingPreviewResponse } from "@/lib/types";

export const orderApi = {
  listMine: (page = 0) => orderClient.request<PageResponse<OrderSummary>>(`/v1/orders/my?page=${page}&size=12&sort=createdAt,desc`),
  get: (id: string) => orderClient.request<OrderDetail>(`/v1/orders/${encodeURIComponent(id)}`),
  paymentMethods: (currency: string, amount: number, country: string) => orderClient.request<PaymentMethodOptionsResponse>(`/v1/orders/payment-methods?currency=${encodeURIComponent(currency)}&amount=${encodeURIComponent(amount.toFixed(2))}&country=${encodeURIComponent(country)}`),
  previewCoupon: (couponCode: string, subtotal: number) => orderClient.json<CouponPreviewResponse, { couponCode: string; subtotal: number }>("/v1/orders/coupon-preview", { couponCode, subtotal }),
  shippingPreview: (payload: { subtotal: number; couponCode?: string; country: string }) => orderClient.json<ShippingPreviewResponse, typeof payload>("/v1/orders/shipping-preview", payload),
  cancel: (id: string) => orderClient.request<OrderDetail>(`/v1/orders/${encodeURIComponent(id)}/cancel`, { method: "POST" }),
  cancelItem: (orderId: string, itemId: string) => orderClient.request<OrderDetail>(`/v1/orders/${encodeURIComponent(orderId)}/items/${encodeURIComponent(itemId)}/cancel`, { method: "POST" }),
  invoiceUrl: (id: string) => `${process.env.NEXT_PUBLIC_ORDER_API_URL ?? "http://localhost:8083"}/api/v1/orders/${encodeURIComponent(id)}/invoice`,
  returns: (page = 0) => orderClient.request<PageResponse<ReturnRequest>>(`/v1/returns/my?page=${page}&size=20`),
  createReturn: (payload: CreateReturnRequest) => orderClient.json<ReturnRequest, CreateReturnRequest>("/v1/returns", payload),
};

export interface ReturnRequest { id: string; returnNumber: string; orderId: string; customerId: string; status: string; comment: string; refundAmount: number; refundStatus: string; createdAt: string; approvedAt: string | null; receivedAt: string | null; completedAt: string | null; items: Array<{ id: string; orderItemId: string; sku: string; quantity: number; reason: string; resolution: string | null }>; }
export type ReturnReason = "DAMAGED" | "WRONG_ITEM" | "DEFECTIVE" | "NOT_AS_DESCRIBED" | "SIZE_FIT" | "OTHER";
export interface CreateReturnRequest { orderId: string; comment?: string; items: Array<{ orderItemId: string; quantity: number; reason: ReturnReason; resolution?: string }>; }
