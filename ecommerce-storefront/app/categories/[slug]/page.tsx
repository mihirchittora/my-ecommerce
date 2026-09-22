import type { Metadata } from "next";
import { titleCase } from "@/lib/utils";
import { catalogApi } from "@/lib/api/catalog";
import { CategoryBrowser } from "@/components/category-browser";

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const { slug } = await params;
  const fallback = titleCase(decodeURIComponent(slug));
  let name = fallback;
  try { name = (await catalogApi.getCategoryBySlug(decodeURIComponent(slug))).name; } catch { /* use the route slug when the catalog is unavailable during rendering */ }
  return { title: name, description: `Browse the ${name} collection at Morrow.`, alternates: { canonical: `/categories/${slug}` }, openGraph: { title: `${name} | Morrow`, description: `Browse the ${name} collection at Morrow.` } };
}

export default async function CategoryPage({ params }: { params: Promise<{ slug: string }> }) { const { slug } = await params; return <CategoryBrowser slug={decodeURIComponent(slug)} />; }
