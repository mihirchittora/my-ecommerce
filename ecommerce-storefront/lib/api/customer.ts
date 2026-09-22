import { customerClient } from "@/lib/api/client";
import type { Address, AddressPayload, CustomerProfile } from "@/lib/types";

export const customerApi = {
  getProfile: () => customerClient.request<CustomerProfile>("/v1/customers/me"),
  updateProfile: (payload: { firstName?: string; lastName?: string; phone?: string }) => customerClient.json<CustomerProfile, typeof payload>("/v1/customers/me", payload, { method: "PATCH" }),
  listAddresses: () => customerClient.request<Address[]>("/v1/customers/me/addresses"),
  createAddress: (payload: AddressPayload) => customerClient.json<Address, AddressPayload>("/v1/customers/me/addresses", payload),
  updateAddress: (id: string, payload: AddressPayload) => customerClient.json<Address, AddressPayload>(`/v1/customers/me/addresses/${encodeURIComponent(id)}`, payload, { method: "PUT" }),
  deleteAddress: (id: string) => customerClient.request<void>(`/v1/customers/me/addresses/${encodeURIComponent(id)}`, { method: "DELETE" }),
  setDefaultAddress: (id: string) => customerClient.request<Address>(`/v1/customers/me/addresses/${encodeURIComponent(id)}/default`, { method: "POST" }),
};
