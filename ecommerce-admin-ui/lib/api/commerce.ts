import { catalogClient, orderClient } from "@/lib/api/client";
import type { PageResponse } from "@/lib/types";

export interface Coupon {
  id: string;
  code: string;
  type: "PERCENTAGE" | "FIXED";
  value: number;
  minimumOrderAmount: number;
  maximumDiscount: number | null;
  usageLimit: number | null;
  perCustomerLimit: number | null;
  usageCount: number;
  active: boolean;
  startsAt: string | null;
  expiresAt: string | null;
}

export interface CouponPayload {
  code: string;
  type: Coupon["type"];
  value: number;
  minimumOrderAmount: number;
  maximumDiscount?: number;
  usageLimit?: number;
  perCustomerLimit?: number;
  active: boolean;
}

export interface ReturnRequest {
  id: string;
  returnNumber: string;
  orderId: string;
  customerId: string;
  status: string;
  refundAmount: number;
  refundStatus: string;
  createdAt: string;
  items: Array<{ orderItemId: string; sku: string; quantity: number; reason: string }>;
}

export interface Review {
  id: string;
  productId: string;
  productName: string | null;
  productSlug: string | null;
  orderId: string;
  orderItemId: string;
  customerId: string;
  sku: string | null;
  rating: number;
  title: string | null;
  comment: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  verifiedPurchase: boolean;
  createdAt: string;
}

export interface ShippingSettings {
  freeShippingThreshold: number;
  standardShippingCharge: number;
  expressShippingCharge: number;
  freeShippingCountries: string;
  updatedAt: string | null;
}

export type ShippingSettingsPayload = Omit<ShippingSettings, "updatedAt">;

export const commerceApi = {
  coupons: () => orderClient.request<PageResponse<Coupon>>("/v1/admin/coupons?page=0&size=100"),
  createCoupon: (payload: CouponPayload) => orderClient.json<Coupon, CouponPayload>("/v1/admin/coupons", payload),
  toggleCoupon: (id: string, active: boolean) => orderClient.request<Coupon>(`/v1/admin/coupons/${encodeURIComponent(id)}/active?active=${active}`, { method: "POST" }),
  returns: () => orderClient.request<PageResponse<ReturnRequest>>("/v1/admin/returns?page=0&size=100"),
  transitionReturn: (id: string, action: string) => orderClient.request<ReturnRequest>(`/v1/admin/returns/${encodeURIComponent(id)}/${encodeURIComponent(action)}`, { method: "POST" }),
  reviews: () => catalogClient.request<PageResponse<Review>>("/v1/admin/reviews?page=0&size=100"),
  moderateReview: (id: string, status: Review["status"]) => catalogClient.request<Review>(`/v1/admin/reviews/${encodeURIComponent(id)}/moderate?status=${status}`, { method: "POST" }),
  shippingSettings: () => orderClient.request<ShippingSettings>("/v1/admin/shipping/settings"),
  updateShippingSettings: (payload: ShippingSettingsPayload) => orderClient.json<ShippingSettings, ShippingSettingsPayload>("/v1/admin/shipping/settings", payload, { method: "PUT" }),
};
