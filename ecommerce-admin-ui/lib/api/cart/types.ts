import type { PageResponse } from "@/lib/types";

export const CART_STATUSES = ["ACTIVE", "CHECKOUT_IN_PROGRESS", "CONVERTED", "ABANDONED", "EXPIRED"] as const;
export type CartStatus = (typeof CART_STATUSES)[number];

export type CartSort =
  | "updatedAt,desc" | "updatedAt,asc"
  | "createdAt,desc" | "createdAt,asc"
  | "expiresAt,asc" | "expiresAt,desc"
  | "status,asc" | "status,desc"
  | "currency,asc" | "currency,desc";

export interface CartListParams {
  page: number;
  size: number;
  sort: CartSort;
  search?: string;
  status?: CartStatus;
  sku?: string;
}

export interface CartSummary {
  id: string;
  customerId: string;
  status: CartStatus;
  currency: string;
  createdAt: string;
  updatedAt: string;
  expiresAt: string | null;
  convertedOrderId: string | null;
  convertedOrderNumber: string | null;
  itemCount: number;
  totalQuantity: number;
}

export interface CartProduct {
  productId: string | null;
  variantId: string | null;
  name: string | null;
  variant: string | null;
  attributes: Record<string, string>;
}

export interface CartPricing {
  unitPrice: number;
  currency: string;
  subtotalEstimate: number;
}

export interface CartAvailability {
  known: boolean;
  availableQuantity: number | null;
  message: string | null;
}

export interface CartItem {
  id: string;
  sku: string;
  quantity: number;
  createdAt: string;
  updatedAt: string;
  product: CartProduct | null;
  pricing: CartPricing | null;
  availability: CartAvailability | null;
  unavailable: boolean;
}

export interface CartDetail extends CartSummary {
  version: number;
  enrichmentAvailable: boolean;
  warnings: string[];
  items: CartItem[];
}

export type CartPage = PageResponse<CartSummary>;

export function isCartStatus(value: string): value is CartStatus {
  return CART_STATUSES.includes(value as CartStatus);
}
