import Link from "next/link";
import { ArrowRight } from "lucide-react";
import type { Category } from "@/lib/types";
import { CategoryImage } from "@/components/category-image";

export function CategoryCard({ category, compact = false, priority = false }: { category: Category; compact?: boolean; priority?: boolean }) {
  return <Link href={`/categories/${encodeURIComponent(category.slug)}`} className="group block overflow-hidden rounded-3xl border border-ink/10 bg-white transition hover:-translate-y-1 hover:shadow-soft">
    <div className={`relative overflow-hidden bg-mist ${compact ? "aspect-[1.35/1]" : "aspect-[1.3/1]"}`}><CategoryImage category={category} priority={priority} sizes={compact ? "(max-width: 640px) 42vw, 20vw" : "(max-width: 640px) 90vw, 33vw"} className="transition duration-500 group-hover:scale-105" /></div>
    <div className={`flex items-center justify-between gap-3 ${compact ? "p-3.5" : "p-5"}`}><div className="min-w-0"><h2 className={`truncate font-display font-bold text-ink ${compact ? "text-base" : "text-xl"}`}>{category.name}</h2>{!compact && category.description ? <p className="mt-1 line-clamp-2 text-sm leading-5 text-ink/55">{category.description}</p> : null}</div><ArrowRight className="h-4 w-4 shrink-0 text-moss transition group-hover:translate-x-1" /></div>
  </Link>;
}
