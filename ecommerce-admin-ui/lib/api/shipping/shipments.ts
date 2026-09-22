import { shippingClient } from "@/lib/api/shipping/client";
import type { CreateShipmentPayload, ShipmentDetail, ShipmentListParams, ShipmentPage } from "@/lib/api/shipping/types";

function query(values: Record<string, string | number | undefined>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== "") params.set(key, String(value));
  });
  const text = params.toString();
  return text ? `?${text}` : "";
}

export const shipmentApi = {
  list: (params: ShipmentListParams) => shippingClient.request<ShipmentPage>(`/v1/shipments${query({ page: params.page, size: params.size, sort: params.sort })}`),
  get: (id: string) => shippingClient.request<ShipmentDetail>(`/v1/shipments/${encodeURIComponent(id)}`),
  create: (payload: CreateShipmentPayload, idempotencyKey: string) => shippingClient.json<ShipmentDetail, CreateShipmentPayload>("/v1/shipments", payload, { headers: { "Idempotency-Key": idempotencyKey } }),
  cancel: (id: string) => shippingClient.request<ShipmentDetail>(`/v1/shipments/${encodeURIComponent(id)}/cancel`, { method: "POST" }),
};

export function newShipmentIdempotencyKey() {
  return `admin-shipment-${typeof crypto !== "undefined" && "randomUUID" in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`}`;
}
