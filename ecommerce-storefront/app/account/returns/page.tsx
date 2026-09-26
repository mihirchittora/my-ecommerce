"use client";

import Link from "next/link";
import { ArrowRight, RefreshCcw } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { orderApi, type ReturnRequest } from "@/lib/api/order";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { EmptyState, ErrorState, LoadingBlock } from "@/components/feedback";
import { formatCurrency, formatDate, titleCase } from "@/lib/utils";

function statusTone(status: string): "neutral" | "success" | "warning" | "danger" {
  if (status === "COMPLETED") return "success";
  if (status === "REJECTED" || status === "CANCELLED") return "danger";
  if (status === "REQUESTED" || status === "APPROVED") return "warning";
  return "neutral";
}

function ReturnCard({ request }: { request: ReturnRequest }) {
  return <Card className="p-5 md:p-6"><div className="flex flex-wrap items-start justify-between gap-4"><div><div className="flex items-center gap-3"><RefreshCcw className="h-5 w-5 text-moss" /><h2 className="font-display text-xl font-bold">{request.returnNumber}</h2></div><p className="mt-2 text-sm text-ink/55">Requested {formatDate(request.createdAt)}</p></div><Badge tone={statusTone(request.status)}>{titleCase(request.status)}</Badge></div><div className="mt-5 grid gap-3 border-t border-ink/10 pt-4">{request.items.map((item) => <div key={item.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-mist px-4 py-3"><div><p className="text-sm font-semibold">{item.sku}</p><p className="mt-1 text-xs text-ink/55">Qty {item.quantity} · {titleCase(item.reason)}</p></div><span className="text-xs font-semibold text-ink/50">{item.resolution ? titleCase(item.resolution) : "Refund"}</span></div>)}</div>{request.comment ? <p className="mt-4 text-sm leading-6 text-ink/60">{request.comment}</p> : null}<div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-ink/10 pt-4 text-sm"><div><span className="text-ink/55">Refund</span><span className="ml-2 font-semibold">{request.refundAmount > 0 ? formatCurrency(request.refundAmount, "INR") : "Calculated after approval"}</span></div><Link href={`/orders/${encodeURIComponent(request.orderId)}`} className="inline-flex items-center gap-2 font-bold text-moss hover:text-ink">View order <ArrowRight className="h-4 w-4" /></Link></div></Card>;
}

export default function ReturnsPage() {
  const returns = useQuery({ queryKey: ["returns", "mine", 0], queryFn: () => orderApi.returns(0) });
  if (returns.isLoading) return <LoadingBlock label="Loading returns" />;
  if (returns.isError) return <ErrorState message="Your return requests are temporarily unavailable." retry={() => void returns.refetch()} />;
  if (!returns.data?.content.length) return <EmptyState title="No return requests yet" message="When an eligible order needs to come back, you can start a return from its order details." action={<Button asLink="/orders">View orders</Button>} />;
  return <div><div className="flex flex-wrap items-end justify-between gap-4"><div><p className="eyebrow">Customer care</p><h2 className="mt-2 font-display text-3xl font-bold">Your returns</h2><p className="mt-3 max-w-xl text-sm leading-6 text-ink/60">Track return approvals, handoff, and refund progress in one place.</p></div><Link href="/orders" className="inline-flex items-center gap-2 text-sm font-bold text-moss hover:text-ink">View orders <ArrowRight className="h-4 w-4" /></Link></div><div className="mt-7 grid gap-4">{returns.data.content.map((request) => <ReturnCard key={request.id} request={request} />)}</div>{returns.data.totalPages > 1 ? <p className="mt-6 text-sm text-ink/55">Showing page {returns.data.number + 1} of {returns.data.totalPages}.</p> : null}</div>;
}
