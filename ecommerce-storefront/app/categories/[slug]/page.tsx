import type { Metadata } from "next";
import { getAssetUrl, titleCase } from "@/lib/utils";
import { catalogApi } from "@/lib/api/catalog";
import { CategoryBrowser } from "@/components/category-browser";

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const { slug } = await params;
  const fallback = titleCase(decodeURIComponent(slug));
  let category: Awaited<ReturnType<typeof catalogApi.getCategoryBySlug>> | undefined;
  try { category = await catalogApi.getCategoryBySlug(decodeURIComponent(slug)); } catch { /* use the route slug when the catalog is unavailable during rendering */ }
  const name = category?.name ?? fallback;
  const description = category?.description || `Browse the ${name} collection at Morrow.`;
  const image = getAssetUrl(category?.image?.url);
  return { title: name, description, alternates: { canonical: `/categories/${slug}` }, openGraph: { title: `${name} | Morrow`, description, ...(image ? { images: [{ url: image, alt: category?.image?.altText || `${name} collection` }] } : {}) } };
}

export default async function CategoryPage({ params }: { params: Promise<{ slug: string }> }) { const { slug } = await params; return <CategoryBrowser slug={decodeURIComponent(slug)} />; }
