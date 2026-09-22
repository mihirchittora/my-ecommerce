import { customerClient } from "@/lib/api/customer/client";
import type { CustomerAddress, CustomerPage, CustomerProfile, CustomerStatus } from "@/lib/api/customer/types";

function query(values: Record<string, string | number | undefined>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== "") params.set(key, String(value));
  });
  const text = params.toString();
  return text ? `?${text}` : "";
}

export const customerApi = {
  getMyProfile: () => customerClient.request<CustomerProfile>("/v1/customers/me"),
  listMyAddresses: () => customerClient.request<CustomerAddress[]>("/v1/customers/me/addresses"),
};

export const customerAdminApi = {
  list: (params: { page: number; size: number; search?: string; status?: CustomerStatus }) => customerClient.request<CustomerPage>(`/v1/customers${query(params)}`),
  get: (id: string) => customerClient.request<CustomerProfile>(`/v1/customers/${encodeURIComponent(id)}`),
  getByAuthUserId: (authUserId: string) => customerClient.request<CustomerProfile>(`/v1/customers/by-auth-user/${encodeURIComponent(authUserId)}`),
  addresses: (id: string) => customerClient.request<CustomerAddress[]>(`/v1/customers/${encodeURIComponent(id)}/addresses`),
  update: (id: string, payload: { firstName?: string; lastName?: string; phone?: string }) => customerClient.json<CustomerProfile, { firstName?: string; lastName?: string; phone?: string }>(`/v1/customers/${encodeURIComponent(id)}`, payload, { method: "PATCH" }),
  updateStatus: (id: string, status: CustomerStatus) => customerClient.json<CustomerProfile, { status: CustomerStatus }>(`/v1/customers/${encodeURIComponent(id)}/status`, { status }, { method: "PATCH" }),
};
