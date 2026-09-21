export const CUSTOMER_STATUSES = ["ACTIVE", "INACTIVE", "BLOCKED"] as const;
export type CustomerStatus = (typeof CUSTOMER_STATUSES)[number];

export interface CustomerProfile {
  id: string;
  authUserId: string;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  phone: string | null;
  status: CustomerStatus;
  createdAt: string;
  updatedAt: string;
}

export type CustomerPage = import("@/lib/types").PageResponse<CustomerProfile>;

export type AddressType = "SHIPPING" | "BILLING";

export interface CustomerAddress {
  id: string;
  customerId: string;
  addressType: AddressType;
  recipientName: string;
  phone: string;
  line1: string;
  line2: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  landmark: string | null;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}
