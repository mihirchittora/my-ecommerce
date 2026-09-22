import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { paymentApi, newPaymentIdempotencyKey } from "@/lib/api/payment/payments";
import type { Payment, PaymentListParams, RefundPayload } from "@/lib/api/payment/types";

export const paymentQueryKeys = {
  list: (params: PaymentListParams) => ["payment-admin", "payments", params] as const,
  detail: (id: string) => ["payment-admin", "payments", id] as const,
};

export function usePayments(params: PaymentListParams, enabled = true) {
  return useQuery({ queryKey: paymentQueryKeys.list(params), queryFn: () => paymentApi.list(params), enabled, placeholderData: (previous) => previous });
}

export function usePayment(id: string, enabled = true) {
  return useQuery({ queryKey: paymentQueryKeys.detail(id), queryFn: () => paymentApi.get(id), enabled: Boolean(id) && enabled });
}

export function useRefundPayment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ paymentId, payload }: { paymentId: string; payload: RefundPayload }) => paymentApi.refund(paymentId, payload, newPaymentIdempotencyKey("refund")),
    onSuccess: (payment) => {
      queryClient.setQueryData(paymentQueryKeys.detail(payment.id), payment);
      void queryClient.invalidateQueries({ queryKey: ["payment-admin", "payments"] });
    },
  });
}

export function useRetryPayment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ paymentId }: { paymentId: string }) => paymentApi.retry(paymentId, newPaymentIdempotencyKey("retry")),
    onSuccess: (payment) => {
      queryClient.setQueryData(paymentQueryKeys.detail(payment.id), payment);
      void queryClient.invalidateQueries({ queryKey: ["payment-admin", "payments"] });
    },
  });
}

export function useCollectCodPayment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ paymentId }: { paymentId: string }) => paymentApi.collect(paymentId),
    onSuccess: (payment: Payment) => {
      queryClient.setQueryData(paymentQueryKeys.detail(payment.id), payment);
      void queryClient.invalidateQueries({ queryKey: ["payment-admin", "payments"] });
    },
  });
}
