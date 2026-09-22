"use client";

import Link from "next/link";
import { CreditCard, ExternalLink } from "lucide-react";
import { ErrorState } from "@/components/feedback-states";
import { PaymentStatusBadge } from "@/components/payments/payment-status-badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/components/auth-provider";
import { usePayments } from "@/lib/api/payment/queries";
import type { OrderDetail } from "@/lib/api/order/types";
import { formatCurrency, formatDate } from "@/lib/utils";

export function OrderPaymentPanel({ order }: { order: OrderDetail }) {
  const { hasPermission } = useAuth();
  const canReadPayments = hasPermission("PAYMENT_READ");
  const payments = usePayments({ page: 0, size: 20, sort: "createdAt,desc", orderId: order.id }, canReadPayments);
  if (!canReadPayments) return null;
  if (payments.isError) return <Card><CardHeader><CardTitle>Payment</CardTitle></CardHeader><CardContent><ErrorState title="Payment service unavailable" message="Payment data could not be loaded. Order details remain available." onRetry={() => void payments.refetch()} /></CardContent></Card>;
  return <Card><CardHeader><div className="flex items-center justify-between gap-3"><div className="flex items-center gap-2"><CreditCard className="h-5 w-5 text-primary" /><div><CardTitle>Payment</CardTitle><p className="mt-1 text-sm text-muted-foreground">Payment state is owned by Payment Service.</p></div></div><Link href={`/payments?search=${encodeURIComponent(order.id)}`} className="inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline">Open payments <ExternalLink className="h-3.5 w-3.5" /></Link></div></CardHeader><CardContent>{payments.isLoading ? <div className="h-16 animate-pulse rounded-xl bg-slate-100" /> : payments.data?.content.length ? <div className="space-y-3">{payments.data.content.map((payment) => <div className="flex flex-col gap-3 rounded-2xl border border-slate-100 p-4 sm:flex-row sm:items-center sm:justify-between" key={payment.id}><div className="min-w-0"><Link href={`/payments/${payment.id}`} className="font-mono text-sm font-semibold text-primary hover:underline">{payment.id}</Link><p className="mt-1 text-xs text-slate-500">{payment.paymentMethod.replaceAll("_", " ")} · {payment.provider ?? "No gateway"} · {formatDate(payment.createdAt)}</p></div><div className="flex flex-wrap items-center gap-4"><PaymentStatusBadge status={payment.status} /><span className="text-sm font-semibold text-slate-800">{formatCurrency(payment.amount, payment.currency)}</span><Link href={`/payments/${payment.id}`} className="text-sm font-semibold text-primary hover:underline">View</Link></div></div>)}</div> : <p className="text-sm text-slate-500">Payment information is not available for this order.</p>}</CardContent></Card>;
}
