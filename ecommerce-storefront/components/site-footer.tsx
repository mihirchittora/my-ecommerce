"use client";

import Link from "next/link";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { catalogApi } from "@/lib/api/catalog";
import { getAssetUrl } from "@/lib/utils";

export function SiteFooter() {
  const siteSettings = useQuery({ queryKey: ["site-settings"], queryFn: catalogApi.getSiteSettings, staleTime: 60_000 });
  const siteTitle = siteSettings.data?.siteTitle ?? "Morrow";
  const logoUrl = getAssetUrl(siteSettings.data?.logoUrl);
  return <footer className="mt-20 border-t border-ink/10 bg-ink text-white"><div className="mx-auto grid max-w-7xl gap-10 px-5 py-14 md:grid-cols-[1.5fr_1fr_1fr_1fr] lg:px-8"><div><div className="flex items-center gap-2"><span className="relative grid h-9 w-9 place-items-center overflow-hidden rounded-2xl bg-coral text-sm font-black">{logoUrl ? <Image src={logoUrl} alt="" fill sizes="36px" unoptimized className="object-contain" /> : siteTitle.slice(0, 1).toUpperCase()}</span><span className="font-display text-lg font-bold">{siteTitle}</span></div><p className="mt-4 max-w-xs text-sm leading-6 text-white/60">A considered collection for everyday living, delivered with care.</p></div><div><h2 className="text-sm font-bold">Shop</h2><div className="mt-4 grid gap-3 text-sm text-white/60"><Link href="/categories" className="hover:text-white">Categories</Link><Link href="/products" className="hover:text-white">All products</Link><Link href="/search" className="hover:text-white">Search</Link></div></div><div><h2 className="text-sm font-bold">Customer Service</h2><div className="mt-4 grid gap-3 text-sm text-white/60"><Link href="/account" className="hover:text-white">My account</Link><Link href="/orders" className="hover:text-white">Order history</Link><span>Support details coming soon</span></div></div><div><h2 className="text-sm font-bold">Policies</h2><div className="mt-4 grid gap-3 text-sm text-white/60"><span>Returns policy coming soon</span><span>Privacy policy coming soon</span><span>Contact information coming soon</span></div></div></div><div className="border-t border-white/10"><div className="mx-auto max-w-7xl px-5 py-5 text-xs text-white/45 lg:px-8">© {new Date().getFullYear()} {siteTitle}. Built for thoughtful shopping.</div></div></footer>;
}
