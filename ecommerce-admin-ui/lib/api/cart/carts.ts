import { cartClient } from "@/lib/api/cart/client";
import type { CartDetail, CartListParams, CartPage } from "@/lib/api/cart/types";

function query(values: Record<string, string | number | undefined>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== "") params.set(key, String(value));
  });
  const text = params.toString();
  return text ? `?${text}` : "";
}

export const cartApi = {
  list: (params: CartListParams) => cartClient.request<CartPage>(`/v1/carts${query({
    page: params.page,
    size: params.size,
    sort: params.sort,
    search: params.search,
    status: params.status,
    sku: params.sku,
  })}`),
  get: (id: string) => cartClient.request<CartDetail>(`/v1/carts/${encodeURIComponent(id)}`),
};
