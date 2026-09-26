"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { commerceApi, type CouponPayload } from "@/lib/api/commerce";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";

const initial: CouponPayload = { code: "", type: "PERCENTAGE", value: 10, minimumOrderAmount: 0, active: true };

export default function CouponsPage() {
  const queryClient = useQueryClient();
  const coupons = useQuery({ queryKey: ["admin", "coupons"], queryFn: commerceApi.coupons });
  const [form, setForm] = useState(initial);
  const create = useMutation({ mutationFn: () => commerceApi.createCoupon({ ...form, code: form.code.trim().toUpperCase() }), onSuccess: () => { setForm(initial); void queryClient.invalidateQueries({ queryKey: ["admin", "coupons"] }); } });
  const toggle = useMutation({ mutationFn: ({ id, active }: { id: string; active: boolean }) => commerceApi.toggleCoupon(id, active), onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["admin", "coupons"] }) });
  return <><PageIntro eyebrow="Commerce operations" title="Coupons" description="Create and disable promotion codes. Discount, shipping and tax totals remain authoritative in Order Service." /><div className="grid gap-5 lg:grid-cols-[360px_1fr]"><Card className="p-5"><h2 className="font-semibold">Create coupon</h2><div className="mt-4 grid gap-3"><label className="text-sm"><span className="mb-1 block font-medium">Code</span><Input value={form.code} onChange={(event) => setForm({ ...form, code: event.target.value })} placeholder="WELCOME10" /></label><label className="text-sm"><span className="mb-1 block font-medium">Type</span><select className="h-10 w-full rounded-xl border border-input bg-background px-3 text-sm" value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as CouponPayload["type"] })}><option value="PERCENTAGE">Percentage</option><option value="FIXED">Fixed amount</option></select></label><label className="text-sm"><span className="mb-1 block font-medium">Value</span><Input type="number" min="0" step="0.01" value={form.value} onChange={(event) => setForm({ ...form, value: Number(event.target.value) })} /></label><label className="text-sm"><span className="mb-1 block font-medium">Minimum order amount</span><Input type="number" min="0" step="0.01" value={form.minimumOrderAmount} onChange={(event) => setForm({ ...form, minimumOrderAmount: Number(event.target.value) })} /></label><Button disabled={!form.code.trim() || create.isPending} onClick={() => create.mutate()}>{create.isPending ? "Creating…" : "Create coupon"}</Button>{create.isError ? <p className="text-sm text-rose-600">Could not create the coupon.</p> : null}</div></Card><div>{coupons.isLoading ? <LoadingCard /> : coupons.isError ? <ErrorState title="Coupons unavailable" message="Order Service could not be reached." onRetry={() => void coupons.refetch()} /> : coupons.data?.content.length ? <Card className="overflow-hidden"><div className="divide-y divide-slate-100">{coupons.data.content.map((coupon) => <div key={coupon.id} className="flex flex-wrap items-center justify-between gap-4 p-5"><div><p className="font-semibold">{coupon.code}</p><p className="mt-1 text-sm text-slate-500">{coupon.type === "PERCENTAGE" ? `${coupon.value}% off` : `${coupon.value} off`} · {coupon.usageCount} uses</p></div><Button variant="outline" size="sm" disabled={toggle.isPending} onClick={() => toggle.mutate({ id: coupon.id, active: !coupon.active })}>{coupon.active ? "Disable" : "Enable"}</Button></div>)}</div></Card> : <EmptyState title="No coupons" message="Create the first promotion code for checkout." />}</div></div></>;
}
