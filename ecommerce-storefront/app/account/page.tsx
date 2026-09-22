"use client";

import Link from "next/link";
import { ArrowRight, LogOut, MapPin, Package, UserRound } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { customerApi } from "@/lib/api/customer";
import { orderApi } from "@/lib/api/order";
import { useAuth } from "@/components/auth-provider";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { LoadingBlock } from "@/components/feedback";

export default function AccountPage() {
  const { user, logout } = useAuth();
  const profile = useQuery({ queryKey: ["customer", "me"], queryFn: customerApi.getProfile });
  const orders = useQuery({ queryKey: ["orders", "mine", 0], queryFn: () => orderApi.listMine(0) });
  if (profile.isLoading) return <LoadingBlock label="Loading your account" />;
  return <div className="grid gap-5 md:grid-cols-3"><Card className="p-6 md:col-span-2"><div className="flex items-start justify-between gap-4"><div><p className="eyebrow">Hello</p><h2 className="mt-2 font-display text-2xl font-bold">{profile.data?.firstName || user?.firstName} {profile.data?.lastName || user?.lastName}</h2><p className="mt-2 text-sm text-ink/60">{profile.data?.email ?? user?.email}</p></div><span className="grid h-12 w-12 place-items-center rounded-2xl bg-sage text-moss"><UserRound className="h-5 w-5" /></span></div><Link href="/account/profile" className="mt-7 inline-flex items-center gap-2 text-sm font-bold text-moss hover:text-ink">Edit profile <ArrowRight className="h-4 w-4" /></Link></Card><Card className="p-6"><Package className="h-6 w-6 text-coral" /><p className="mt-5 text-sm text-ink/55">Orders placed</p><p className="mt-1 font-display text-3xl font-bold">{orders.data?.totalElements ?? "—"}</p><Link href="/orders" className="mt-5 inline-flex items-center gap-2 text-sm font-bold text-moss">View orders <ArrowRight className="h-4 w-4" /></Link></Card><Card className="p-6 md:col-span-2"><MapPin className="h-6 w-6 text-moss" /><h2 className="mt-5 font-display text-xl font-bold">Your addresses</h2><p className="mt-2 text-sm leading-6 text-ink/60">Keep shipping details ready for a faster checkout.</p><Link href="/account/addresses" className="mt-5 inline-flex items-center gap-2 text-sm font-bold text-moss">Manage addresses <ArrowRight className="h-4 w-4" /></Link></Card><Card className="flex items-center justify-between gap-4 p-6"><div><h2 className="font-display text-xl font-bold">Sign out</h2><p className="mt-2 text-sm text-ink/60">End this session on this device.</p></div><Button variant="ghost" aria-label="Sign out" onClick={() => void logout()}><LogOut className="h-5 w-5" /></Button></Card></div>;
}
