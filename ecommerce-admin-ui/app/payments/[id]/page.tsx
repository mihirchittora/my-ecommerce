"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useParams } from "next/navigation";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { useAuth } from "@/components/auth-provider";
import { PaymentAttempts, PaymentCustomerPanel, PaymentGatewayPanel, PaymentOrderPanel, PaymentOverview, PaymentRefunds } from "@/components/payments/payment-detail-sections";
import { PaymentActions } from "@/components/payments/payment-actions";
import { PaymentStatusBadge } from "@/components/payments/payment-status-badge";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/client";
import { useCustomer } from "@/lib/api/customer/queries";
import { useOrder } from "@/lib/api/order/queries";
import { usePayment } from "@/lib/api/payment/queries";
import { formatDate } from "@/lib/utils";

function errorState(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 403) return { title: "Payment access denied", message: "You do not have permission to view payments." };
    if (error.status === 404) return { title: "Payment not found", message: "The payment may have been removed or the link may be invalid." };
    if (error.status === 0 || error.status >= 500) return { title: "Payment Service unavailable", message: "Payment Service is temporarily unavailable." };
    return { title: "Unable to load payment", message: error.message };
  }
  return { title: "Unable to load payment", message: "Payment Service is temporarily unavailable." };
}

function dependencyMessage(error: unknown, name: string) {
  if (error instanceof ApiError && error.status === 403) return `You do not have permission to view ${name.toLowerCase()} details.`;
  if (error instanceof ApiError && error.status === 404) return `${name} information is unavailable for this reference.`;
  return `${name} information is temporarily unavailable.`;
}

export default function PaymentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { hasPermission } = useAuth();
  const payment = usePayment(id);
  const canReadOrder = hasPermission("ORDER_READ");
  const canReadCustomer = hasPermission("CUSTOMER_READ");
  const order = useOrder(payment.data?.orderId ?? "", canReadOrder && Boolean(payment.data));
  const customer = useCustomer(payment.data?.customerId ?? "", canReadCustomer && Boolean(payment.data));

  if (payment.isLoading) return <LoadingCard rows={8} />;
  if (payment.isError || !payment.data) {
    const error = errorState(payment.error);
    return <ErrorState title={error.title} message={error.message} onRetry={() => void payment.refetch()} />;
  }

  const target = payment.data;
  return <><div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/payments"><ArrowLeft className="h-4 w-4" />Back to payments</Link></Button></div><div className="mb-8 flex flex-col gap-4 md:flex-row md:items-end md:justify-between"><div><p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">Payment detail</p><div className="flex flex-wrap items-center gap-3"><h1 className="font-mono text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">{target.id}</h1><PaymentStatusBadge status={target.status} /></div><p className="mt-2 text-sm text-slate-500">{target.provider} · Created {formatDate(target.createdAt)}</p></div><PaymentActions payment={target} /></div><div className="space-y-6"><PaymentOverview payment={target} order={order.data} />{canReadOrder ? <PaymentOrderPanel payment={target} order={order.data} errorMessage={order.isError ? dependencyMessage(order.error, "Order") : undefined} /> : <PaymentOrderPanel payment={target} errorMessage="Order details require ORDER_READ permission." />}{canReadCustomer ? <PaymentCustomerPanel payment={target} customer={customer.data} errorMessage={customer.isError ? dependencyMessage(customer.error, "Customer") : undefined} /> : <PaymentCustomerPanel payment={target} errorMessage="Customer details require CUSTOMER_READ permission." />}<PaymentGatewayPanel payment={target} /><PaymentAttempts attempts={target.attempts} /><PaymentRefunds refunds={target.refunds} /></div></>;
}
