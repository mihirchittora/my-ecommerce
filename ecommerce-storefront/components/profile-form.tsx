"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { customerApi } from "@/lib/api/customer";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { ErrorState, LoadingBlock } from "@/components/feedback";

export function ProfileForm() {
  const queryClient = useQueryClient();
  const profile = useQuery({ queryKey: ["customer", "me"], queryFn: customerApi.getProfile });
  const [firstName, setFirstName] = useState<string | null>(null);
  const [lastName, setLastName] = useState<string | null>(null);
  const [phone, setPhone] = useState<string | null>(null);
  const update = useMutation({ mutationFn: () => customerApi.updateProfile({ firstName: firstName ?? profile.data?.firstName, lastName: lastName ?? profile.data?.lastName, phone: phone ?? profile.data?.phone ?? undefined }), onSuccess: (data) => { queryClient.setQueryData(["customer", "me"], data); } });
  if (profile.isLoading) return <LoadingBlock label="Loading profile" />;
  if (profile.isError || !profile.data) return <ErrorState message="Your profile is temporarily unavailable." retry={() => void profile.refetch()} />;
  return <Card className="max-w-2xl p-6 md:p-8"><div className="grid gap-5 sm:grid-cols-2"><label><span className="field-label">First name</span><Input value={firstName ?? profile.data.firstName} onChange={(event) => setFirstName(event.target.value)} autoComplete="given-name" /></label><label><span className="field-label">Last name</span><Input value={lastName ?? profile.data.lastName} onChange={(event) => setLastName(event.target.value)} autoComplete="family-name" /></label><label className="sm:col-span-2"><span className="field-label">Email</span><Input value={profile.data.email} readOnly className="bg-mist text-ink/55" /><span className="mt-2 block text-xs text-ink/45">Email and authentication details are managed by your secure sign-in service.</span></label><label className="sm:col-span-2"><span className="field-label">Phone</span><Input value={phone ?? profile.data.phone ?? ""} onChange={(event) => setPhone(event.target.value)} autoComplete="tel" placeholder="+1 555 123 4567" /></label></div>{update.isError ? <p role="alert" className="mt-5 rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-800">We could not save your profile. Please check the fields and try again.</p> : null}{update.isSuccess ? <p className="mt-5 text-sm font-semibold text-moss">Profile saved.</p> : null}<Button className="mt-7" disabled={update.isPending} onClick={() => update.mutate()}>{update.isPending ? "Saving…" : "Save changes"}</Button></Card>;
}
