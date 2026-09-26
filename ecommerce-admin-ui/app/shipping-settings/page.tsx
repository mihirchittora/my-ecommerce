"use client";

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { commerceApi, type ShippingSettingsPayload } from "@/lib/api/commerce";
import { useAuth } from "@/components/auth-provider";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { hasPermission } from "@/lib/permissions";

const defaults: ShippingSettingsPayload = {
  freeShippingThreshold: 500,
  standardShippingCharge: 79,
  expressShippingCharge: 149,
  freeShippingCountries: "IN",
};

export default function ShippingSettingsPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const settings = useQuery({ queryKey: ["admin", "shipping-settings"], queryFn: commerceApi.shippingSettings });
  const [form, setForm] = useState<ShippingSettingsPayload>(defaults);
  const canManage = hasPermission(user, "SHIPPING_MANAGE");

  useEffect(() => {
    if (settings.data) {
      setForm({
        freeShippingThreshold: settings.data.freeShippingThreshold,
        standardShippingCharge: settings.data.standardShippingCharge,
        expressShippingCharge: settings.data.expressShippingCharge,
        freeShippingCountries: settings.data.freeShippingCountries,
      });
    }
  }, [settings.data]);

  const update = useMutation({
    mutationFn: () => commerceApi.updateShippingSettings({
      ...form,
      freeShippingCountries: form.freeShippingCountries.trim().toUpperCase(),
    }),
    onSuccess: (next) => {
      setForm({
        freeShippingThreshold: next.freeShippingThreshold,
        standardShippingCharge: next.standardShippingCharge,
        expressShippingCharge: next.expressShippingCharge,
        freeShippingCountries: next.freeShippingCountries,
      });
      void queryClient.invalidateQueries({ queryKey: ["admin", "shipping-settings"] });
    },
  });

  if (settings.isLoading) return <LoadingCard rows={5} />;
  if (settings.isError || !settings.data) return <ErrorState title="Shipping settings unavailable" message="Order Service could not be reached." onRetry={() => void settings.refetch()} />;

  return <>
    <PageIntro eyebrow="Commerce operations" title="Shipping settings" description="Change the shipping threshold, delivery charges, and countries eligible for free standard shipping. New values apply to new checkout calculations immediately." />
    <Card className="max-w-3xl p-6">
      <div className="grid gap-5 sm:grid-cols-2">
        <label className="text-sm"><span className="mb-1 block font-medium">Free shipping threshold</span><Input type="number" min="0" step="0.01" value={form.freeShippingThreshold} disabled={!canManage} onChange={(event) => setForm({ ...form, freeShippingThreshold: Number(event.target.value) })} /><span className="mt-1 block text-xs text-muted-foreground">Standard shipping is free when the eligible-country merchandise total is greater than this amount.</span></label>
        <label className="text-sm"><span className="mb-1 block font-medium">Standard shipping charge</span><Input type="number" min="0" step="0.01" value={form.standardShippingCharge} disabled={!canManage} onChange={(event) => setForm({ ...form, standardShippingCharge: Number(event.target.value) })} /></label>
        <label className="text-sm"><span className="mb-1 block font-medium">Express shipping charge</span><Input type="number" min="0" step="0.01" value={form.expressShippingCharge} disabled={!canManage} onChange={(event) => setForm({ ...form, expressShippingCharge: Number(event.target.value) })} /></label>
        <label className="text-sm"><span className="mb-1 block font-medium">Free shipping countries</span><Input value={form.freeShippingCountries} disabled={!canManage} onChange={(event) => setForm({ ...form, freeShippingCountries: event.target.value })} placeholder="IN,US" /><span className="mt-1 block text-xs text-muted-foreground">Comma-separated ISO country codes.</span></label>
      </div>
      {update.isError ? <p role="alert" className="mt-5 rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">Could not save shipping settings. Check the values and try again.</p> : null}
      {canManage ? <Button className="mt-6" disabled={update.isPending || !form.freeShippingCountries.trim()} onClick={() => update.mutate()}>{update.isPending ? "Saving…" : "Save shipping settings"}</Button> : <p className="mt-6 text-sm text-slate-500">You have read-only access. Shipping settings changes require SHIPPING_MANAGE.</p>}
    </Card>
  </>;
}
