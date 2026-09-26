"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { commerceApi } from "@/lib/api/commerce";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";

export default function ReturnsPage() {
  const queryClient = useQueryClient();
  const returns = useQuery({ queryKey: ["admin", "returns"], queryFn: commerceApi.returns });
  const transition = useMutation({ mutationFn: ({ id, action }: { id: string; action: string }) => commerceApi.transitionReturn(id, action), onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["admin", "returns"] }) });
  return <><PageIntro eyebrow="Customer care" title="Returns" description="Review return requests and advance them through the return and refund lifecycle." />{returns.isLoading ? <LoadingCard /> : returns.isError ? <ErrorState title="Returns unavailable" message="Order Service could not be reached." onRetry={() => void returns.refetch()} /> : returns.data?.content.length ? <Card className="overflow-hidden"><div className="divide-y divide-slate-100">{returns.data.content.map((item) => <div key={item.id} className="flex flex-wrap items-center justify-between gap-4 p-5"><div><p className="font-semibold">{item.returnNumber}</p><p className="mt-1 text-sm text-slate-500">Order {item.orderId} · {item.status} · refund {item.refundStatus}</p><p className="mt-1 text-xs text-slate-400">{item.items.map((line) => `${line.sku} × ${line.quantity}`).join(", ")}</p></div><div className="flex flex-wrap gap-2"><Button size="sm" variant="outline" disabled={transition.isPending || item.status !== "REQUESTED"} onClick={() => transition.mutate({ id: item.id, action: "APPROVED" })}>Approve</Button><Button size="sm" variant="destructive" disabled={transition.isPending || item.status !== "REQUESTED"} onClick={() => transition.mutate({ id: item.id, action: "REJECTED" })}>Reject</Button><Button size="sm" variant="outline" disabled={transition.isPending || !["APPROVED", "IN_TRANSIT"].includes(item.status)} onClick={() => transition.mutate({ id: item.id, action: "RECEIVED" })}>Mark received</Button><Button size="sm" disabled={transition.isPending || item.status !== "RECEIVED"} onClick={() => transition.mutate({ id: item.id, action: "COMPLETED" })}>Complete</Button></div></div>)}</div></Card> : <EmptyState title="No return requests" message="Customer return requests will appear here." />}</>;
}
