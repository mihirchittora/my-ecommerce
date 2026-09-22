import { shippingClient } from "@/lib/api/shipping/client";
import type { TrackingResponse } from "@/lib/api/shipping/types";

export const trackingApi = {
  get: (shipmentId: string) => shippingClient.request<TrackingResponse>(`/v1/shipments/${encodeURIComponent(shipmentId)}/tracking`),
};
