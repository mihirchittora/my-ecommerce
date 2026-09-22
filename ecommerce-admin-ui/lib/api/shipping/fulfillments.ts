import { shippingClient } from "@/lib/api/shipping/client";
import type { FulfillmentDetail, FulfillmentListParams, FulfillmentPage } from "@/lib/api/shipping/types";

function query(values: Record<string, string | number | undefined>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== "") params.set(key, String(value));
  });
  const text = params.toString();
  return text ? `?${text}` : "";
}

export const fulfillmentApi = {
  list: (params: FulfillmentListParams) => shippingClient.request<FulfillmentPage>(`/v1/fulfillments${query({ page: params.page, size: params.size, sort: params.sort })}`),
  get: (id: string) => shippingClient.request<FulfillmentDetail>(`/v1/fulfillments/${encodeURIComponent(id)}`),
  getByOrder: (orderId: string) => shippingClient.request<FulfillmentDetail>(`/v1/fulfillments/order/${encodeURIComponent(orderId)}`),
};
