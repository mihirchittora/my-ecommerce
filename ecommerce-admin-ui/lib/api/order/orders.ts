import { orderClient } from "@/lib/api/order/client";
import type { OrderDetail, OrderListParams, OrderPage } from "@/lib/api/order/types";

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

export const orderApi = {
  list: (params: OrderListParams) => orderClient.request<OrderPage>(`/v1/orders${query({
    page: params.page,
    size: params.size,
    sort: params.sort,
    orderNumber: params.orderNumber,
    status: params.status,
    sku: params.sku,
    createdFrom: dayStart(params.createdFrom),
    createdTo: dayEndExclusive(params.createdTo),
  })}`),
  get: (id: string) => orderClient.request<OrderDetail>(`/v1/orders/${encodeURIComponent(id)}`),
  cancel: (id: string) => orderClient.request<OrderDetail>(`/v1/orders/${encodeURIComponent(id)}/cancel`, { method: "POST" }),
};
