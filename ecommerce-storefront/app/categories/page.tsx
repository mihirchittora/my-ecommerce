"use client";

import Link from "next/link";
import { ArrowRight, Sparkles } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { catalogApi } from "@/lib/api/catalog";
import { EmptyState, ErrorState, LoadingBlock } from "@/components/feedback";

export default function CategoriesPage() {
  const categories = useQuery({ queryKey: ["categories", "root"], queryFn: () => catalogApi.listCategories() });
  return <div className="page-shell py-12 md:py-16"><p className="eyebrow">Explore the collection</p><h1 className="mt-3 font-display text-4xl font-bold tracking-tight md:text-6xl">Categories</h1><p className="mt-5 max-w-xl text-base leading-7 text-ink/60">Start with a category, then narrow down to the pieces that fit your everyday.</p><div className="mt-10">{categories.isLoading ? <LoadingBlock label="Loading categories" /> : categories.isError ? <ErrorState message="Categories are temporarily unavailable." retry={() => void categories.refetch()} /> : categories.data?.length ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{categories.data.map((category, index) => <Link key={category.id} href={`/categories/${encodeURIComponent(category.slug)}`} className={`group min-h-52 rounded-3xl p-7 transition hover:-translate-y-1 hover:shadow-soft ${index % 3 === 0 ? "bg-sage" : index % 3 === 1 ? "bg-[#f3dfd5]" : "bg-[#e8e4d5]"}`}><span className="grid h-11 w-11 place-items-center rounded-2xl bg-white/60 text-moss"><Sparkles className="h-5 w-5" /></span><div className="mt-14 flex items-center justify-between gap-4"><h2 className="font-display text-2xl font-bold">{category.name}</h2><ArrowRight className="h-5 w-5 transition group-hover:translate-x-1" /></div></Link>)}</div> : <EmptyState title="No categories yet" message="Published categories will appear here when they are available." />}</div></div>;
}
