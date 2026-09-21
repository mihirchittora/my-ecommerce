import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { customerAdminApi } from "@/lib/api/customer/customers";
import type { CustomerStatus } from "@/lib/api/customer/types";

export const customerQueryKeys = {
  list: (params: { page: number; size: number; search?: string; status?: CustomerStatus }) => ["customer-admin", "customers", params] as const,
  detail: (id: string) => ["customer-admin", "customers", id] as const,
  addresses: (id: string) => ["customer-admin", "customers", id, "addresses"] as const,
};

export function useCustomers(params: { page: number; size: number; search?: string; status?: CustomerStatus }, enabled = true) {
  return useQuery({ queryKey: customerQueryKeys.list(params), queryFn: () => customerAdminApi.list(params), enabled, placeholderData: (previous) => previous });
}

export function useCustomer(id: string, enabled = true) {
  return useQuery({ queryKey: customerQueryKeys.detail(id), queryFn: () => customerAdminApi.get(id), enabled: Boolean(id) && enabled });
}

export function useCustomerAddresses(id: string, enabled = true) {
  return useQuery({ queryKey: customerQueryKeys.addresses(id), queryFn: () => customerAdminApi.addresses(id), enabled: Boolean(id) && enabled });
}

export function useUpdateCustomer() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ id, payload }: { id: string; payload: { firstName?: string; lastName?: string; phone?: string } }) => customerAdminApi.update(id, payload), onSuccess: (customer) => { queryClient.setQueryData(customerQueryKeys.detail(customer.id), customer); void queryClient.invalidateQueries({ queryKey: ["customer-admin", "customers"] }); } });
}

export function useUpdateCustomerStatus() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ id, status }: { id: string; status: CustomerStatus }) => customerAdminApi.updateStatus(id, status), onSuccess: (customer) => { queryClient.setQueryData(customerQueryKeys.detail(customer.id), customer); void queryClient.invalidateQueries({ queryKey: ["customer-admin", "customers"] }); } });
}
