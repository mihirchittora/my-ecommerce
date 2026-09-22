"use client";

import Link from "next/link";
import { Banknote, RotateCcw, Undo2 } from "lucide-react";
import { useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useCollectCodPayment, useRefundPayment, useRetryPayment } from "@/lib/api/payment/queries";
import { canCollectCod, canRefundPayment, canRetryPayment } from "@/lib/api/payment/action-rules";
import type { Payment } from "@/lib/api/payment/types";
import { ApiError } from "@/lib/api/client";
import { formatCurrency } from "@/lib/utils";
import { useToast } from "@/components/ui/toast";

function actionError(error: unknown, operation: "refund" | "retry") {
  if (error instanceof ApiError) {
    if (error.status === 403) return "You do not have permission to perform this action.";
    if (error.status === 409) return operation === "refund" ? "This payment cannot be refunded in its current state or for that amount." : "Only failed payments can be retried.";
    if (error.status === 503 || error.status === 0) return "Payment Service or the provider is temporarily unavailable.";
    return error.message;
  }
  return operation === "refund" ? "The refund could not be completed." : "The payment could not be retried.";
}

export function PaymentActions({ payment }: { payment: Payment }) {
  const { user } = useAuth();
  const permissions = user?.permissions ?? [];
  return <div className="flex flex-wrap items-center gap-2">{canCollectCod(payment, permissions) && <CollectCodButton payment={payment} />} {canRefundPayment(payment.status, permissions) && <RefundDialog payment={payment} />} {canRetryPayment(payment.status, permissions) && <RetryPaymentButton payment={payment} />}</div>;
}

function CollectCodButton({ payment }: { payment: Payment }) {
  const collect = useCollectCodPayment();
  const { toast } = useToast();
  const execute = () => {
    if (!window.confirm(`Confirm that ${payment.amount.toFixed(2)} ${payment.currency} was collected for this delivered order?`)) return;
    collect.mutate({ paymentId: payment.id }, {
      onSuccess: () => toast({ title: "COD collected", description: "The payment is now marked paid and the order can complete." }),
      onError: (error) => toast({ title: "COD collection failed", description: error instanceof ApiError ? error.message : "The payment could not be collected.", variant: "destructive" }),
    });
  };
  return <Button variant="outline" size="sm" className="text-emerald-700 hover:text-emerald-800" onClick={execute} disabled={collect.isPending}><Banknote className="h-4 w-4" />{collect.isPending ? "Collecting…" : "Collect COD"}</Button>;
}

function RetryPaymentButton({ payment }: { payment: Payment }) {
  const retry = useRetryPayment();
  const { toast } = useToast();
  const execute = () => {
    if (!window.confirm(`Retry payment ${payment.id}? A new provider attempt will be created.`)) return;
    retry.mutate({ paymentId: payment.id }, {
      onSuccess: () => toast({ title: "Payment retry started", description: `${payment.id} received a new provider attempt.` }),
      onError: (error) => toast({ title: "Payment retry failed", description: actionError(error, "retry"), variant: "destructive" }),
    });
  };
  return <Button variant="outline" size="sm" onClick={execute} disabled={retry.isPending}><RotateCcw className="h-4 w-4" />{retry.isPending ? "Retrying…" : "Retry"}</Button>;
}

function RefundDialog({ payment }: { payment: Payment }) {
  const [open, setOpen] = useState(false);
  const [amount, setAmount] = useState("");
  const [reason, setReason] = useState("");
  const refund = useRefundPayment();
  const { toast } = useToast();
  const remaining = Math.max(0, Number((payment.amount - payment.refundedAmount).toFixed(2)));
  const parsedAmount = amount === "" ? NaN : Number(amount);
  const amountError = amount !== "" && (!Number.isFinite(parsedAmount) || parsedAmount <= 0 || parsedAmount > remaining)
    ? `Enter an amount greater than zero and no more than ${formatCurrency(remaining, payment.currency)}.`
    : null;
  const submit = () => {
    if (amountError || !Number.isFinite(parsedAmount) || parsedAmount <= 0 || parsedAmount > remaining) return;
    refund.mutate({ paymentId: payment.id, payload: { amount: Number(parsedAmount.toFixed(2)), reason: reason.trim() || undefined } }, {
      onSuccess: (next) => {
        setOpen(false);
        setAmount("");
        setReason("");
        toast({ title: "Refund requested", description: `${formatCurrency(parsedAmount, payment.currency)} was submitted for ${next.id}.` });
      },
    });
  };
  return <><Button variant="outline" size="sm" className="text-rose-600 hover:text-rose-700" onClick={() => { setAmount(""); setReason(""); setOpen(true); }}><Undo2 className="h-4 w-4" />Refund</Button><Dialog open={open} onOpenChange={(nextOpen) => { if (!refund.isPending) setOpen(nextOpen); }}><DialogContent><DialogHeader><DialogTitle>Refund payment</DialogTitle><DialogDescription>Refunds are executed by the configured gateway through Payment Service. This action cannot be reversed here.</DialogDescription></DialogHeader><div className="grid gap-4 rounded-2xl bg-slate-50 p-4 text-sm"><div className="flex items-center justify-between gap-4"><span className="text-slate-500">Payment</span><Link href={`/payments/${payment.id}`} className="font-mono font-semibold text-primary hover:underline">{payment.id}</Link></div><div className="flex items-center justify-between gap-4"><span className="text-slate-500">Order</span><Link href={`/orders/${payment.orderId}`} className="font-mono font-semibold text-primary hover:underline">{payment.orderId}</Link></div><div className="flex items-center justify-between gap-4"><span className="text-slate-500">Captured amount</span><span className="font-semibold text-slate-800">{formatCurrency(payment.amount, payment.currency)}</span></div><div className="flex items-center justify-between gap-4"><span className="text-slate-500">Already refunded</span><span className="font-semibold text-slate-800">{formatCurrency(payment.refundedAmount, payment.currency)}</span></div><div className="flex items-center justify-between gap-4 border-t border-slate-200 pt-3"><span className="font-semibold text-slate-700">Remaining refundable</span><span className="font-bold text-slate-950">{formatCurrency(remaining, payment.currency)}</span></div></div><div><Label htmlFor={`refund-amount-${payment.id}`}>Refund amount</Label><div className="mt-2 flex gap-2"><Input id={`refund-amount-${payment.id}`} type="number" min="0.01" max={remaining} step="0.01" inputMode="decimal" value={amount} onChange={(event) => setAmount(event.target.value)} aria-invalid={Boolean(amountError)} placeholder={remaining.toFixed(2)} /><Button type="button" variant="outline" onClick={() => setAmount(remaining.toFixed(2))} disabled={remaining <= 0}>Refund full amount</Button></div>{amountError && <p role="alert" className="mt-2 text-sm text-rose-600">{amountError}</p>}</div><div><Label htmlFor={`refund-reason-${payment.id}`}>Reason <span className="font-normal text-slate-400">(optional)</span></Label><Textarea id={`refund-reason-${payment.id}`} className="mt-2" maxLength={500} value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Customer request" /></div>{refund.error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{actionError(refund.error, "refund")}</p>}<DialogFooter><Button variant="outline" onClick={() => setOpen(false)} disabled={refund.isPending}>Cancel</Button><Button variant="destructive" onClick={submit} disabled={refund.isPending || Boolean(amountError) || !Number.isFinite(parsedAmount) || parsedAmount <= 0 || parsedAmount > remaining}>{refund.isPending ? "Submitting…" : "Refund payment"}</Button></DialogFooter></DialogContent></Dialog></>;
}
