import { cartClient } from "@/lib/api/client";
import type { Cart, CheckoutResponse, PaymentMethod } from "@/lib/types";

export const cartApi = {
  get: () => cartClient.request<Cart>("/v1/cart"),
  addItem: (sku: string, quantity: number) => cartClient.json<Cart, { sku: string; quantity: number }>("/v1/cart/items", { sku, quantity }),
  updateItem: (itemId: string, quantity: number) => cartClient.json<Cart, { quantity: number }>(`/v1/cart/items/${encodeURIComponent(itemId)}`, { quantity }, { method: "PATCH" }),
  removeItem: (itemId: string) => cartClient.request<Cart>(`/v1/cart/items/${encodeURIComponent(itemId)}`, { method: "DELETE" }),
  clear: () => cartClient.request<Cart>("/v1/cart", { method: "DELETE" }),
  checkout: (payload: { currency: string; preferredLocationId?: string; paymentMethod: PaymentMethod; couponCode?: string; serviceLevel?: "STANDARD" | "EXPRESS"; shippingAddress: { sourceAddressId?: string; recipientName: string; phone: string; line1: string; line2?: string; city: string; state: string; postalCode: string; country: string; landmark?: string } }, idempotencyKey: string) => cartClient.json<CheckoutResponse, typeof payload>("/v1/cart/checkout", payload, { headers: { "Idempotency-Key": idempotencyKey } }),
};
