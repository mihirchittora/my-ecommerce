import { useQueries, useQuery, type UseQueryResult } from "@tanstack/react-query";
import { cartApi } from "@/lib/api/cart/carts";
import type { CartDetail, CartListParams, CartPage } from "@/lib/api/cart/types";
import { inventoryApi } from "@/lib/api/inventory";
import type { InventorySkuSummary } from "@/lib/types";

export const cartQueryKeys = {
  carts: ["carts"] as const,
  cart: (id: string) => ["cart", id] as const,
  catalog: (id: string) => ["cart", id, "catalog"] as const,
  inventory: (id: string, sku: string) => ["cart", id, "inventory", sku] as const,
};

export function useCartList(params: CartListParams, enabled = true) {
  return useQuery<CartPage>({
    queryKey: [...cartQueryKeys.carts, params],
    queryFn: () => cartApi.list(params),
    enabled,
    placeholderData: (previous) => previous,
  });
}

export function useCart(id: string) {
  return useQuery<CartDetail>({
    queryKey: cartQueryKeys.cart(id),
    queryFn: () => cartApi.get(id),
    enabled: Boolean(id),
  });
}

export interface CartInventoryQuery {
  sku: string;
  query: UseQueryResult<InventorySkuSummary, Error>;
}

export function useCartInventory(cartId: string, skus: string[], enabled: boolean): CartInventoryQuery[] {
  const uniqueSkus = Array.from(new Set(skus));
  const results = useQueries({
    queries: uniqueSkus.map((sku) => ({
      queryKey: cartQueryKeys.inventory(cartId, sku),
      queryFn: () => inventoryApi.summary(sku),
      enabled: Boolean(cartId && sku && enabled),
    })),
  });
  return uniqueSkus.map((sku, index) => ({ sku, query: results[index] as UseQueryResult<InventorySkuSummary, Error> }));
}
