"use client";

import Link from "next/link";
import type { LucideIcon } from "lucide-react";
import { ArrowRight, BadgeCheck, Coffee, HeartHandshake, Home, Palette, RotateCcw, Shirt, ShoppingBag, Smartphone, Sparkles, Truck, WalletCards, WandSparkles } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { catalogApi } from "@/lib/api/catalog";
import { ProductGrid } from "@/components/product-card";
import { EmptyState, ErrorState, ProductSkeletonGrid } from "@/components/feedback";

const categoryIcons: Record<string, LucideIcon> = {
  coffee: Coffee,
  phones: Smartphone,
  electronics: Smartphone,
  home: Home,
  fashion: Shirt,
  beauty: WandSparkles,
  accessories: Palette,
};

function CategoryIcon({ slug }: { slug: string }) {
  const Icon = categoryIcons[slug.toLowerCase()] ?? Sparkles;
  return <Icon className="h-6 w-6" aria-hidden="true" />;
}

export default function HomePage() {
  const categories = useQuery({ queryKey: ["categories", "root"], queryFn: () => catalogApi.listCategories() });
  const products = useQuery({ queryKey: ["products", "featured"], queryFn: () => catalogApi.listProducts({ page: 0, size: 8, sort: "createdAt,desc" }) });
  const rootCategories = categories.data?.filter((category) => !category.parentId).slice(0, 8) ?? [];

  return <>
    <section className="page-shell pt-5 md:pt-8">
      <div className="relative overflow-hidden rounded-3xl bg-[#f2d5ce] px-6 py-8 sm:px-10 md:px-14 md:py-12">
        <div className="absolute -right-16 -top-28 h-72 w-72 rounded-full bg-white/40 blur-3xl" />
        <div className="absolute -bottom-32 right-1/3 h-64 w-64 rounded-full bg-coral/20 blur-3xl" />
        <div className="relative grid items-center gap-8 lg:grid-cols-[1.15fr_0.85fr]">
          <div className="max-w-2xl">
            <div className="inline-flex items-center gap-2 rounded-full bg-white/70 px-3 py-1.5 text-xs font-bold text-moss"><BadgeCheck className="h-4 w-4" /> Thoughtful shopping, made simple</div>
            <h1 className="mt-5 max-w-xl font-display text-4xl font-bold leading-[1.05] tracking-tight text-ink md:text-6xl">Everyday goods, at prices that feel good.</h1>
            <p className="mt-5 max-w-lg text-base leading-7 text-ink/65 md:text-lg">Browse useful, beautiful pieces across the categories you reach for most.</p>
            <div className="mt-7 flex flex-wrap gap-3"><Link href="/products" className="inline-flex min-h-11 items-center rounded-full bg-ink px-6 text-sm font-bold text-white transition hover:bg-moss">Shop all products <ArrowRight className="ml-2 h-4 w-4" /></Link><Link href="/categories" className="inline-flex min-h-11 items-center rounded-full border border-ink/15 bg-white/70 px-6 text-sm font-bold text-ink transition hover:bg-white">Explore categories</Link></div>
          </div>
          <div className="hidden justify-self-end sm:block"><div className="grid w-64 grid-cols-2 gap-3"><div className="flex aspect-square flex-col justify-between rounded-3xl bg-white/75 p-5 shadow-sm"><Coffee className="h-7 w-7 text-moss" /><span className="font-display text-lg font-bold">Small rituals</span></div><div className="mt-8 flex aspect-square flex-col justify-between rounded-3xl bg-ink p-5 text-white shadow-sm"><ShoppingBag className="h-7 w-7 text-coral" /><span className="font-display text-lg font-bold">Better finds</span></div></div></div>
        </div>
      </div>
    </section>

    <section className="page-shell pt-10 md:pt-14">
      <div className="flex items-end justify-between gap-5"><div><p className="eyebrow">Browse the catalogue</p><h2 className="mt-2 font-display text-2xl font-bold tracking-tight md:text-3xl">Shop by category</h2></div><Link href="/categories" className="hidden items-center gap-2 text-sm font-bold text-moss hover:text-ink sm:flex">View all <ArrowRight className="h-4 w-4" /></Link></div>
      {categories.isLoading ? <div className="mt-6 grid grid-flow-col auto-cols-[minmax(156px,1fr)] gap-3 overflow-hidden sm:grid-cols-2 sm:grid-flow-row md:grid-cols-4"><div className="h-36 animate-pulse rounded-2xl bg-ink/10" /><div className="h-36 animate-pulse rounded-2xl bg-ink/10" /><div className="h-36 animate-pulse rounded-2xl bg-ink/10" /><div className="h-36 animate-pulse rounded-2xl bg-ink/10" /></div> : categories.isError ? <div className="mt-6"><ErrorState message="Categories are temporarily unavailable." retry={() => void categories.refetch()} /></div> : rootCategories.length ? <div className="mt-6 grid grid-flow-col auto-cols-[minmax(156px,1fr)] gap-3 overflow-x-auto pb-2 scrollbar-none sm:grid-cols-2 sm:grid-flow-row md:grid-cols-4">{rootCategories.map((category, index) => <Link key={category.id} href={`/categories/${encodeURIComponent(category.slug)}`} className={`group flex min-h-36 flex-col justify-between rounded-2xl p-5 transition hover:-translate-y-0.5 hover:shadow-soft ${index % 4 === 0 ? "bg-sage" : index % 4 === 1 ? "bg-[#f3dfd5]" : index % 4 === 2 ? "bg-[#e8e4d5]" : "bg-[#e5e0f1]"}`}><span className="grid h-11 w-11 place-items-center rounded-2xl bg-white/70 text-moss"><CategoryIcon slug={category.slug} /></span><span className="flex items-center justify-between gap-2 font-display text-lg font-bold text-ink">{category.name}<ArrowRight className="h-4 w-4 transition group-hover:translate-x-1" /></span></Link>)}</div> : <div className="mt-6"><EmptyState title="Categories are coming soon" message="Browse the full collection while the catalogue grows." /></div>}
    </section>

    <section className="mt-12 border-y border-ink/10 bg-white md:mt-16"><div className="page-shell py-10 md:py-14"><div className="flex items-end justify-between gap-5"><div><p className="eyebrow">Fresh from the collection</p><h2 className="mt-2 font-display text-2xl font-bold tracking-tight md:text-3xl">Top picks for you</h2></div><Link href="/products" className="hidden items-center gap-2 text-sm font-bold text-moss hover:text-ink sm:flex">Shop all <ArrowRight className="h-4 w-4" /></Link></div><div className="mt-7">{products.isLoading ? <ProductSkeletonGrid /> : products.isError ? <ErrorState message="The collection is temporarily unavailable." retry={() => void products.refetch()} /> : products.data?.content.length ? <ProductGrid products={products.data.content} /> : <EmptyState title="The collection is taking shape" message="There are no published products to show yet." />}</div></div></section>

    <section className="page-shell py-10 md:py-14"><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4"><div className="flex gap-3 rounded-2xl bg-sage p-5"><WalletCards className="h-5 w-5 shrink-0 text-moss" /><div><h2 className="text-sm font-bold">Clear prices</h2><p className="mt-1 text-xs leading-5 text-ink/60">What you see is what you pay.</p></div></div><div className="flex gap-3 rounded-2xl bg-[#f3dfd5] p-5"><Truck className="h-5 w-5 shrink-0 text-coral" /><div><h2 className="text-sm font-bold">Reliable delivery</h2><p className="mt-1 text-xs leading-5 text-ink/60">Live availability before checkout.</p></div></div><div className="flex gap-3 rounded-2xl bg-[#e8e4d5] p-5"><RotateCcw className="h-5 w-5 shrink-0 text-moss" /><div><h2 className="text-sm font-bold">Easy to revisit</h2><p className="mt-1 text-xs leading-5 text-ink/60">Your cart and orders stay together.</p></div></div><div className="flex gap-3 rounded-2xl bg-[#e5e0f1] p-5"><HeartHandshake className="h-5 w-5 shrink-0 text-moss" /><div><h2 className="text-sm font-bold">Chosen with care</h2><p className="mt-1 text-xs leading-5 text-ink/60">A considered collection, not clutter.</p></div></div></div></section>
  </>;
}
