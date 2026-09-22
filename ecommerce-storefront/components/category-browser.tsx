"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { catalogApi } from "@/lib/api/catalog";
import { ProductsBrowser } from "@/components/products-browser";
import { ErrorState, ProductSkeletonGrid } from "@/components/feedback";

export function CategoryBrowser({ slug }: { slug: string }) {
  const category = useQuery({ queryKey: ["category", slug], queryFn: () => catalogApi.getCategoryBySlug(slug) });
  const children = useQuery({ queryKey: ["categories", category.data?.id], queryFn: () => catalogApi.listCategories(category.data?.id), enabled: Boolean(category.data?.id) });
  if (category.isLoading) return <div className="page-shell py-12 md:py-16"><ProductSkeletonGrid /></div>;
  if (category.isError || !category.data) return <div className="page-shell py-12 md:py-16"><ErrorState message="This category could not be loaded." retry={() => void category.refetch()} /></div>;
  return <><div className="page-shell pt-10 md:pt-16"><Link href="/categories" className="inline-flex items-center gap-2 text-sm font-bold text-moss hover:text-ink"><ArrowLeft className="h-4 w-4" />All categories</Link><div className="mt-10 max-w-2xl"><p className="eyebrow">Category</p><h1 className="mt-3 font-display text-4xl font-bold tracking-tight md:text-6xl">{category.data.name}</h1><p className="mt-5 text-base leading-7 text-ink/60">Explore the current products in this category.</p></div>{children.data?.length ? <nav aria-label="Subcategories" className="mt-8 flex flex-wrap gap-2">{children.data.map((child) => <Link key={child.id} href={`/categories/${encodeURIComponent(child.slug)}`} className="rounded-full border border-ink/10 bg-white px-4 py-2 text-sm font-semibold hover:border-moss">{child.name}</Link>)}</nav> : null}</div><ProductsBrowser initialCategoryId={category.data.id} categoryHeading={category.data.name} /></>;
}
