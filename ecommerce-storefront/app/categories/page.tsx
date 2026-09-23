"use client";

import { useQuery } from "@tanstack/react-query";
import { catalogApi } from "@/lib/api/catalog";
import { EmptyState, ErrorState, LoadingBlock } from "@/components/feedback";
import { CategoryCard } from "@/components/category-card";

export default function CategoriesPage() {
  const categories = useQuery({ queryKey: ["categories", "root"], queryFn: () => catalogApi.listCategories() });
  return <div className="page-shell py-12 md:py-16"><p className="eyebrow">Explore the collection</p><h1 className="mt-3 font-display text-4xl font-bold tracking-tight md:text-6xl">Categories</h1><p className="mt-5 max-w-xl text-base leading-7 text-ink/60">Start with a category, then narrow down to the pieces that fit your everyday.</p><div className="mt-10">{categories.isLoading ? <LoadingBlock label="Loading categories" /> : categories.isError ? <ErrorState message="Categories are temporarily unavailable." retry={() => void categories.refetch()} /> : categories.data?.length ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{categories.data.map((category) => <CategoryCard key={category.id} category={category} />)}</div> : <EmptyState title="No categories yet" message="Published categories will appear here when they are available." />}</div></div>;
}
