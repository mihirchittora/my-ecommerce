import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fulfillmentApi } from "@/lib/api/shipping/fulfillments";
import { newShipmentIdempotencyKey, shipmentApi } from "@/lib/api/shipping/shipments";
import { trackingApi } from "@/lib/api/shipping/tracking";
import type { CreateShipmentPayload, FulfillmentListParams, ShipmentListParams } from "@/lib/api/shipping/types";

export const shippingQueryKeys = {
  shipments: (params: ShipmentListParams) => ["shipments", params] as const,
  shipment: (id: string) => ["shipment", id] as const,
  tracking: (id: string) => ["shipment", id, "tracking"] as const,
  fulfillments: (params: FulfillmentListParams) => ["fulfillments", params] as const,
  fulfillment: (id: string) => ["fulfillment", id] as const,
  fulfillmentByOrder: (orderId: string) => ["fulfillment-by-order", orderId] as const,
};

export function useShipments(params: ShipmentListParams) {
  return useQuery({ queryKey: shippingQueryKeys.shipments(params), queryFn: () => shipmentApi.list(params), placeholderData: (previous) => previous });
}

export function useShipment(id: string, enabled = true) {
  return useQuery({ queryKey: shippingQueryKeys.shipment(id), queryFn: () => shipmentApi.get(id), enabled: Boolean(id) && enabled });
}

export function useShipmentTracking(id: string, enabled = true) {
  return useQuery({ queryKey: shippingQueryKeys.tracking(id), queryFn: () => trackingApi.get(id), enabled: Boolean(id) && enabled });
}

export function useFulfillments(params: FulfillmentListParams) {
  return useQuery({ queryKey: shippingQueryKeys.fulfillments(params), queryFn: () => fulfillmentApi.list(params), placeholderData: (previous) => previous });
}

export function useFulfillment(id: string, enabled = true) {
  return useQuery({ queryKey: shippingQueryKeys.fulfillment(id), queryFn: () => fulfillmentApi.get(id), enabled: Boolean(id) && enabled });
}

export function useFulfillmentByOrder(orderId: string, enabled = true) {
  return useQuery({ queryKey: shippingQueryKeys.fulfillmentByOrder(orderId), queryFn: () => fulfillmentApi.getByOrder(orderId), enabled: Boolean(orderId) && enabled, retry: false });
}

export function useCreateShipment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ payload, idempotencyKey }: { payload: CreateShipmentPayload; idempotencyKey?: string }) => shipmentApi.create(payload, idempotencyKey ?? newShipmentIdempotencyKey()),
    onSuccess: (shipment) => {
      queryClient.setQueryData(shippingQueryKeys.shipment(shipment.id), shipment);
      void queryClient.invalidateQueries({ queryKey: ["shipments"] });
      void queryClient.invalidateQueries({ queryKey: ["fulfillments"] });
      void queryClient.invalidateQueries({ queryKey: shippingQueryKeys.fulfillment(shipment.fulfillmentId) });
    },
  });
}

export function useCancelShipment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ shipmentId }: { shipmentId: string }) => shipmentApi.cancel(shipmentId),
    onSuccess: (shipment) => {
      queryClient.setQueryData(shippingQueryKeys.shipment(shipment.id), shipment);
      void queryClient.invalidateQueries({ queryKey: ["shipments"] });
      void queryClient.invalidateQueries({ queryKey: ["fulfillments"] });
      void queryClient.invalidateQueries({ queryKey: shippingQueryKeys.fulfillment(shipment.fulfillmentId) });
    },
  });
}
