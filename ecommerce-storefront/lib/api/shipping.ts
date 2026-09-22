import { shippingClient } from "@/lib/api/client";
import type { PageResponse, ShipmentSummary, TrackingResponse } from "@/lib/types";

export const shippingApi = {
  listMine: () => shippingClient.request<PageResponse<ShipmentSummary>>("/v1/shipments/my?page=0&size=100&sort=createdAt,desc"),
  tracking: (shipmentId: string) => shippingClient.request<TrackingResponse>(`/v1/shipments/${encodeURIComponent(shipmentId)}/tracking`),
};
