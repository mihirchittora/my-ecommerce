import type { Metadata } from "next";
import { ProductDetail } from "@/components/product-detail";
import { catalogApi } from "@/lib/api/catalog";

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const { slug } = await params;
  try {
    const product = await catalogApi.getProductBySlug(decodeURIComponent(slug));
    const description = product.description?.slice(0, 155) ?? `Shop ${product.name} at Morrow.`;
    return { title: product.name, description, alternates: { canonical: `/products/${slug}` }, openGraph: { title: `${product.name} | Morrow`, description, type: "website", images: product.images[0]?.url ? [{ url: product.images[0].url, alt: product.name }] : undefined }, other: { "product:brand": product.brand ?? "Morrow", "product:availability": product.status === "ACTIVE" ? "in stock" : "out of stock" } };
  } catch {
    return { title: "Product", alternates: { canonical: `/products/${slug}` } };
  }
}

export default async function ProductPage({ params }: { params: Promise<{ slug: string }> }) { const { slug } = await params; return <ProductDetail slug={decodeURIComponent(slug)} />; }
