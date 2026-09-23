"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { catalogApi } from "@/lib/api/catalog";
import { ProductsBrowser } from "@/components/products-browser";
import { ErrorState, ProductSkeletonGrid } from "@/components/feedback";
import { CategoryImage } from "@/components/category-image";
import { CategoryCard } from "@/components/category-card";

export function CategoryBrowser({ slug }: { slug: string }) {
  const category = useQuery({ queryKey: ["category", slug], queryFn: () => catalogApi.getCategoryBySlug(slug) });
  const children = useQuery({ queryKey: ["categories", category.data?.id], queryFn: () => catalogApi.listCategories(category.data?.id), enabled: Boolean(category.data?.id) });
  if (category.isLoading) return <div className="page-shell py-12 md:py-16"><ProductSkeletonGrid /></div>;
  if (category.isError || !category.data) return <div className="page-shell py-12 md:py-16"><ErrorState message="This category could not be loaded." retry={() => void category.refetch()} /></div>;
  return <><div className="page-shell pt-10 md:pt-16"><Link href="/categories" className="inline-flex items-center gap-2 text-sm font-bold text-moss hover:text-ink"><ArrowLeft className="h-4 w-4" />All categories</Link><div className="mt-8 grid items-center gap-8 lg:grid-cols-[0.85fr_1.15fr]"><div className="relative aspect-[1.35/1] overflow-hidden rounded-[2rem] bg-mist"><CategoryImage category={category.data} priority sizes="(max-width: 1024px) 100vw, 42vw" /></div><div className="max-w-2xl"><p className="eyebrow">Category</p><h1 className="mt-3 font-display text-4xl font-bold tracking-tight md:text-6xl">{category.data.name}</h1><p className="mt-5 text-base leading-7 text-ink/60">{category.data.description || `Explore the ${category.data.name.toLowerCase()} collection, selected for everyday living.`}</p></div></div>{children.data?.length ? <section className="mt-10" aria-labelledby="subcategory-heading"><div className="flex items-end justify-between gap-4"><div><p className="eyebrow">Keep exploring</p><h2 id="subcategory-heading" className="mt-2 font-display text-2xl font-bold tracking-tight">Shop by subcategory</h2></div></div><div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">{children.data.map((child) => <CategoryCard key={child.id} category={child} compact />)}</div></section> : null}</div><ProductsBrowser initialCategoryId={category.data.id} categoryHeading={category.data.name} /></>;
}
