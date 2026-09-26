"use client";

import Link from "next/link";
import { ArrowRight, HeartHandshake, RotateCcw, Truck, WalletCards } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { catalogApi } from "@/lib/api/catalog";
import { ProductGrid } from "@/components/product-card";
import { EmptyState, ErrorState, ProductSkeletonGrid } from "@/components/feedback";
import { CategoryCard } from "@/components/category-card";
import { HomeHeroCarousel } from "@/components/home-hero-carousel";

export default function HomePage() {
  const categories = useQuery({ queryKey: ["categories", "root"], queryFn: () => catalogApi.listCategories() });
  const products = useQuery({ queryKey: ["products", "featured"], queryFn: () => catalogApi.listProducts({ page: 0, size: 8, sort: "createdAt,desc" }) });
  const siteSettings = useQuery({ queryKey: ["site-settings"], queryFn: catalogApi.getSiteSettings, staleTime: 60_000 });
  const rootCategories = categories.data?.filter((category) => !category.parentId).slice(0, 8) ?? [];

  return <>
    <HomeHeroCarousel slides={siteSettings.data?.slides} />

    <section className="page-shell pt-10 md:pt-14">
      <div className="flex items-end justify-between gap-5"><div><p className="eyebrow">Browse the catalogue</p><h2 className="mt-2 font-display text-2xl font-bold tracking-tight md:text-3xl">Shop by category</h2></div><Link href="/categories" className="hidden items-center gap-2 text-sm font-bold text-moss hover:text-ink sm:flex">View all <ArrowRight className="h-4 w-4" /></Link></div>
      {categories.isLoading ? <div className="mt-6 grid grid-flow-col auto-cols-[minmax(172px,1fr)] gap-3 overflow-hidden sm:grid-cols-2 sm:grid-flow-row md:grid-cols-4"><div className="h-52 animate-pulse rounded-3xl bg-ink/10" /><div className="h-52 animate-pulse rounded-3xl bg-ink/10" /><div className="h-52 animate-pulse rounded-3xl bg-ink/10" /><div className="h-52 animate-pulse rounded-3xl bg-ink/10" /></div> : categories.isError ? <div className="mt-6"><ErrorState message="Categories are temporarily unavailable." retry={() => void categories.refetch()} /></div> : rootCategories.length ? <div className="mt-6 grid grid-flow-col auto-cols-[minmax(172px,1fr)] gap-3 overflow-x-auto pb-2 scrollbar-none sm:grid-cols-2 sm:grid-flow-row md:grid-cols-4">{rootCategories.map((category, index) => <CategoryCard key={category.id} category={category} priority={index < 4} />)}</div> : <div className="mt-6"><EmptyState title="Categories are coming soon" message="Browse the full collection while the catalogue grows." /></div>}
    </section>

    <section className="mt-12 border-y border-ink/10 bg-white md:mt-16"><div className="page-shell py-10 md:py-14"><div className="flex items-end justify-between gap-5"><div><p className="eyebrow">Fresh from the collection</p><h2 className="mt-2 font-display text-2xl font-bold tracking-tight md:text-3xl">Top picks for you</h2></div><Link href="/products" className="hidden items-center gap-2 text-sm font-bold text-moss hover:text-ink sm:flex">Shop all <ArrowRight className="h-4 w-4" /></Link></div><div className="mt-7">{products.isLoading ? <ProductSkeletonGrid /> : products.isError ? <ErrorState message="The collection is temporarily unavailable." retry={() => void products.refetch()} /> : products.data?.content.length ? <ProductGrid products={products.data.content} /> : <EmptyState title="The collection is taking shape" message="There are no published products to show yet." />}</div></div></section>

    <section className="page-shell py-10 md:py-14"><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4"><div className="flex gap-3 rounded-2xl bg-sage p-5"><WalletCards className="h-5 w-5 shrink-0 text-moss" /><div><h2 className="text-sm font-bold">Clear prices</h2><p className="mt-1 text-xs leading-5 text-ink/60">What you see is what you pay.</p></div></div><div className="flex gap-3 rounded-2xl bg-[#f3dfd5] p-5"><Truck className="h-5 w-5 shrink-0 text-coral" /><div><h2 className="text-sm font-bold">Reliable delivery</h2><p className="mt-1 text-xs leading-5 text-ink/60">Live availability before checkout.</p></div></div><div className="flex gap-3 rounded-2xl bg-[#e8e4d5] p-5"><RotateCcw className="h-5 w-5 shrink-0 text-moss" /><div><h2 className="text-sm font-bold">Easy to revisit</h2><p className="mt-1 text-xs leading-5 text-ink/60">Your cart and orders stay together.</p></div></div><div className="flex gap-3 rounded-2xl bg-[#e5e0f1] p-5"><HeartHandshake className="h-5 w-5 shrink-0 text-moss" /><div><h2 className="text-sm font-bold">Chosen with care</h2><p className="mt-1 text-xs leading-5 text-ink/60">A considered collection, not clutter.</p></div></div></div></section>
  </>;
}
