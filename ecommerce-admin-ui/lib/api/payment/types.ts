import type { PageResponse } from "@/lib/types";

export const PAYMENT_STATUSES = [
  "CREATED",
  "PENDING",
  "AUTHORIZED",
  "CAPTURED",
  "PENDING_COLLECTION",
  "FAILED",
  "CANCELLED",
  "REFUND_PENDING",
  "PARTIALLY_REFUNDED",
  "REFUNDED",
] as const;

export type PaymentStatus = (typeof PAYMENT_STATUSES)[number];
export const PAYMENT_PROVIDERS = ["SANDBOX"] as const;
export type PaymentProvider = (typeof PAYMENT_PROVIDERS)[number];
export const PAYMENT_METHODS = ["ONLINE", "CASH_ON_DELIVERY"] as const;
export type PaymentMethod = (typeof PAYMENT_METHODS)[number];
export const REFUND_STATUSES = ["PENDING", "SUCCEEDED", "FAILED"] as const;
export type RefundStatus = (typeof REFUND_STATUSES)[number];
export const PAYMENT_ATTEMPT_STATUSES = ["CREATED", "PENDING", "AUTHORIZED", "CAPTURED", "FAILED", "CANCELLED"] as const;
export type PaymentAttemptStatus = (typeof PAYMENT_ATTEMPT_STATUSES)[number];

export type PaymentSort =
  | "createdAt,desc"
  | "createdAt,asc"
  | "updatedAt,desc"
  | "updatedAt,asc"
  | "amount,desc"
  | "amount,asc"
  | "status,asc"
  | "status,desc"
  | "provider,asc"
  | "provider,desc"
  | "refundedAmount,desc"
  | "refundedAmount,asc";

export interface PaymentListParams {
  page: number;
  size: number;
  sort: PaymentSort;
  search?: string;
  status?: PaymentStatus;
  provider?: PaymentProvider;
  paymentMethod?: PaymentMethod;
  currency?: string;
  orderId?: string;
  customerId?: string;
  createdFrom?: string;
  createdTo?: string;
}

export interface PaymentAttempt {
  id: string;
  attemptNumber: number;
  provider: PaymentProvider;
  status: PaymentAttemptStatus;
  providerPaymentId: string | null;
  providerOrderId: string | null;
  failureCode: string | null;
  failureMessage: string | null;
  amount: number;
  currency: string;
  startedAt: string;
  completedAt: string | null;
  createdAt: string;
}

export interface PaymentRefund {
  id: string;
  amount: number;
  currency: string;
  status: RefundStatus;
  providerRefundId: string | null;
  idempotencyKey: string;
  reason: string | null;
  failureCode: string | null;
  failureMessage: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
}

export interface Payment {
  id: string;
  orderId: string;
  customerId: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  paymentMethod: PaymentMethod;
  provider: PaymentProvider | null;
  providerPaymentId: string | null;
  providerOrderId: string | null;
  refundedAmount: number;
  createdAt: string;
  updatedAt: string;
  authorizedAt: string | null;
  capturedAt: string | null;
  failedAt: string | null;
  cancelledAt: string | null;
  attempts: PaymentAttempt[];
  refunds: PaymentRefund[];
}

export type PaymentPage = PageResponse<Payment>;

export interface RefundPayload {
  amount?: number;
  reason?: string;
}

export function isPaymentStatus(value: string): value is PaymentStatus {
  return PAYMENT_STATUSES.includes(value as PaymentStatus);
}

export function isPaymentProvider(value: string): value is PaymentProvider {
  return PAYMENT_PROVIDERS.includes(value as PaymentProvider);
}
