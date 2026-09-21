"use client";

import Link from "next/link";
import { ArrowLeft, MapPin, Pencil, Save } from "lucide-react";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { useAuth } from "@/components/auth-provider";
import { PageIntro } from "@/components/page-intro";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";
import { useCustomer, useCustomerAddresses, useUpdateCustomer, useUpdateCustomerStatus } from "@/lib/api/customer/queries";
import type { CustomerAddress } from "@/lib/api/customer/types";
import { formatDate } from "@/lib/utils";

function addressCard(address: CustomerAddress) {
  return <article className="rounded-xl border border-slate-100 p-4" key={address.id}><div className="flex items-start justify-between gap-3"><div className="flex items-center gap-2"><MapPin className="h-4 w-4 text-primary" /><p className="text-sm font-semibold text-slate-800">{address.recipientName}</p></div><div className="flex gap-2"><Badge variant="outline">{address.addressType}</Badge>{address.isDefault && <Badge variant="success">Default</Badge>}</div></div><p className="mt-3 text-sm leading-6 text-slate-600">{address.line1}{address.line2 ? `, ${address.line2}` : ""}<br />{address.city}, {address.state} {address.postalCode}<br />{address.country}</p><p className="mt-2 text-xs text-slate-400">{address.phone}{address.landmark ? ` · ${address.landmark}` : ""}</p></article>;
}

export default function CustomerDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { hasPermission } = useAuth();
  const customer = useCustomer(id);
  const addresses = useCustomerAddresses(id, Boolean(customer.data));
  const update = useUpdateCustomer();
  const status = useUpdateCustomerStatus();
  const [editing, setEditing] = useState(false);
  const [profile, setProfile] = useState({ firstName: "", lastName: "", phone: "" });

  useEffect(() => { if (customer.data) setProfile({ firstName: customer.data.firstName ?? "", lastName: customer.data.lastName ?? "", phone: customer.data.phone ?? "" }); }, [customer.data]);

  if (customer.isLoading) return <LoadingCard rows={8} />;
  if (customer.isError || !customer.data) return <ErrorState title="Customer unavailable" message={customer.error instanceof ApiError && customer.error.status === 403 ? "You do not have CUSTOMER_READ permission." : customer.error instanceof ApiError ? customer.error.message : "Customer Service is temporarily unavailable."} onRetry={() => void customer.refetch()} />;

  const target = customer.data;
  const canUpdate = hasPermission("CUSTOMER_UPDATE");
  const saveProfile = async (event: React.FormEvent<HTMLFormElement>) => { event.preventDefault(); await update.mutateAsync({ id, payload: { firstName: profile.firstName, lastName: profile.lastName, phone: profile.phone || undefined } }); setEditing(false); };
  const setStatus = async (next: typeof target.status) => { if (!window.confirm(`Change this customer to ${next}?`)) return; await status.mutateAsync({ id, status: next }); };

  return <><div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/customers"><ArrowLeft className="h-4 w-4" />Back to customers</Link></Button></div><PageIntro eyebrow="User Management / Customers" title={`${target.firstName ?? ""} ${target.lastName ?? ""}`.trim() || "Unnamed customer"} description={target.email || "Customer email is managed by Auth and is not synced into this profile yet."} /><div className="mb-8 flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-card sm:flex-row sm:items-center sm:justify-between"><div><p className="text-xs uppercase tracking-[0.12em] text-slate-400">Account status</p><Badge className="mt-2" variant={target.status === "ACTIVE" ? "success" : target.status === "BLOCKED" ? "danger" : "muted"}>{target.status}</Badge></div><div className="flex flex-wrap gap-2">{canUpdate && target.status !== "ACTIVE" && <Button onClick={() => void setStatus("ACTIVE")}>Activate</Button>}{canUpdate && target.status === "ACTIVE" && <Button variant="outline" onClick={() => void setStatus("INACTIVE")}>Deactivate</Button>}{canUpdate && target.status !== "BLOCKED" && <Button variant="destructive" onClick={() => void setStatus("BLOCKED")}>Block</Button>}{canUpdate && <Button variant="outline" onClick={() => setEditing((value) => !value)}><Pencil className="h-4 w-4" />{editing ? "Close edit" : "Edit profile"}</Button>}</div></div><div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]"><Card><CardHeader><CardTitle>Customer profile</CardTitle></CardHeader><CardContent>{editing ? <form className="space-y-4" onSubmit={(event) => void saveProfile(event)}><div><Label htmlFor="firstName">First name</Label><Input id="firstName" value={profile.firstName} onChange={(event) => setProfile((current) => ({ ...current, firstName: event.target.value }))} maxLength={80} className="mt-2" /></div><div><Label htmlFor="lastName">Last name</Label><Input id="lastName" value={profile.lastName} onChange={(event) => setProfile((current) => ({ ...current, lastName: event.target.value }))} maxLength={80} className="mt-2" /></div><div><Label htmlFor="phone">Phone</Label><Input id="phone" value={profile.phone} onChange={(event) => setProfile((current) => ({ ...current, phone: event.target.value }))} placeholder="+919876543210" maxLength={30} className="mt-2" /></div><div className="flex justify-end gap-2"><Button type="button" variant="outline" onClick={() => setEditing(false)}>Cancel</Button><Button type="submit" disabled={update.isPending}><Save className="h-4 w-4" />{update.isPending ? "Saving…" : "Save changes"}</Button></div>{update.error && <p className="text-sm text-rose-600">{update.error instanceof ApiError ? update.error.message : "Unable to update this customer."}</p>}</form> : <dl className="grid gap-4 sm:grid-cols-2"><div><dt className="text-xs uppercase tracking-[0.1em] text-slate-400">First name</dt><dd className="mt-1 text-sm font-semibold text-slate-800">{target.firstName || "Not set"}</dd></div><div><dt className="text-xs uppercase tracking-[0.1em] text-slate-400">Last name</dt><dd className="mt-1 text-sm font-semibold text-slate-800">{target.lastName || "Not set"}</dd></div><div className="sm:col-span-2"><dt className="text-xs uppercase tracking-[0.1em] text-slate-400">Email</dt><dd className="mt-1 break-all text-sm font-semibold text-slate-800">{target.email || "Owned by Auth; not synced"}</dd></div><div><dt className="text-xs uppercase tracking-[0.1em] text-slate-400">Phone</dt><dd className="mt-1 text-sm text-slate-700">{target.phone || "Not set"}</dd></div><div><dt className="text-xs uppercase tracking-[0.1em] text-slate-400">Created</dt><dd className="mt-1 text-sm text-slate-700">{formatDate(target.createdAt)}</dd></div></dl>}</CardContent></Card><Card><CardHeader><CardTitle>Shipping and billing addresses</CardTitle><p className="text-sm text-muted-foreground">Addresses are owned by Customer Service and are displayed read-only in this admin view.</p></CardHeader><CardContent>{addresses.isLoading ? <LoadingCard rows={3} /> : addresses.isError ? <ErrorState title="Addresses unavailable" message={addresses.error instanceof ApiError ? addresses.error.message : "Customer addresses could not be loaded."} onRetry={() => void addresses.refetch()} /> : addresses.data?.length ? <div className="space-y-4">{addresses.data.map(addressCard)}</div> : <p className="text-sm text-slate-500">This customer has no saved shipping or billing addresses.</p>}</CardContent></Card></div><p className="mt-6 break-all font-mono text-xs text-slate-400">Customer ID: {target.id} · Auth user ID: {target.authUserId}</p></>;
}
