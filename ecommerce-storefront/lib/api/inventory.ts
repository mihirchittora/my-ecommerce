import { inventoryClient } from "@/lib/api/client";
import type { Availability } from "@/lib/types";

export const inventoryApi = {
  getAvailability: (sku: string) => inventoryClient.request<Availability>(`/v1/inventory/availability/${encodeURIComponent(sku)}`),
};
