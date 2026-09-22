import { useMutation, useQueries, useQuery, useQueryClient, type UseQueryResult } from "@tanstack/react-query";
import { inventoryApi } from "@/lib/api/inventory";
import { orderApi } from "@/lib/api/order/orders";
import type { OrderDetail, OrderItem, OrderListParams, OrderPage } from "@/lib/api/order/types";
import type { InventoryReservation } from "@/lib/types";

export const orderQueryKeys = {
  orders: ["orders"] as const,
  order: (id: string) => ["order", id] as const,
  reservation: (id: string) => ["reservation", id] as const,
};

export function useOrderList(params: OrderListParams, enabled = true) {
  return useQuery<OrderPage>({ queryKey: [...orderQueryKeys.orders, params], queryFn: () => orderApi.list(params), enabled, placeholderData: (previous) => previous });
}

export function useOrder(id: string, enabled = true) {
  return useQuery<OrderDetail>({ queryKey: orderQueryKeys.order(id), queryFn: () => orderApi.get(id), enabled: Boolean(id) && enabled });
}

export function useCancelOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ orderId }: { orderId: string; reservationIds?: string[] }) => orderApi.cancel(orderId),
    onSuccess: (order, variables) => {
      queryClient.setQueryData(orderQueryKeys.order(variables.orderId), order);
      queryClient.invalidateQueries({ queryKey: orderQueryKeys.orders });
      variables.reservationIds?.forEach((reservationId) => {
        queryClient.invalidateQueries({ queryKey: orderQueryKeys.reservation(reservationId) });
      });
      queryClient.invalidateQueries({ queryKey: ["reservations"] });
      queryClient.invalidateQueries({ queryKey: ["inventory"] });
    },
  });
}

export interface OrderReservationQuery {
  id: string;
  query: UseQueryResult<InventoryReservation, Error>;
}

export function useOrderReservations(items: OrderItem[]): OrderReservationQuery[] {
  const ids = Array.from(new Set(items.map((item) => item.reservationId).filter((id): id is string => Boolean(id))));
  const results = useQueries({
    queries: ids.map((id) => ({
      queryKey: orderQueryKeys.reservation(id),
      queryFn: () => inventoryApi.reservation(id),
    })),
  });
  return ids.map((id, index) => ({ id, query: results[index] as UseQueryResult<InventoryReservation, Error> }));
}
