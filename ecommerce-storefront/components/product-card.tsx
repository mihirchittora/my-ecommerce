"use client";

import Link from "next/link";
import { ArrowUpRight, ChevronLeft, ChevronRight } from "lucide-react";
import { useQueries } from "@tanstack/react-query";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import type { Product } from "@/lib/types";
import { inventoryApi } from "@/lib/api/inventory";
import { formatCurrency, getAssetUrl, variantLabel } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { CatalogImage } from "@/components/catalog-image";
import { productIsOutOfStock } from "@/lib/storefront-logic";
import { catalogApi } from "@/lib/api/catalog";
import { ReviewRating } from "@/components/review-rating";

export function ProductCard({ product }: { product: Product }) {
  const variants = product.variants.filter((item) => item.status === "ACTIVE");
  const variant = variants[0] ?? product.variants[0];
  const images = [...product.images].sort((a, b) => a.sortOrder - b.sortOrder);
  const [imageIndex, setImageIndex] = useState(0);
  const availabilityQueries = useQueries({ queries: variants.map((item) => ({ queryKey: ["availability", item.sku], queryFn: () => inventoryApi.getAvailability(item.sku), staleTime: 30_000 })) });
  const reviewSummary = useQuery({ queryKey: ["reviews", product.id, "summary"], queryFn: () => catalogApi.reviewSummary(product.id), staleTime: 60_000 });
  const availability = Object.fromEntries(availabilityQueries.map((query, index) => [variants[index]?.sku, query.data]));
  const outOfStock = productIsOutOfStock(product, availability);
  const checkingAvailability = !outOfStock && availabilityQueries.some((query) => query.isPending);
  const inStock = availabilityQueries.some((query) => query.data?.available === true);
  const image = getAssetUrl(images[imageIndex]?.url);
  const lowestPrice = variants.length ? Math.min(...variants.map((item) => item.priceIncludingTax ?? (item.price + (item.taxAmount ?? 0)))) : (variant ? variant.priceIncludingTax ?? (variant.price + (variant.taxAmount ?? 0)) : undefined);
  const moveImage = (direction: 1 | -1) => setImageIndex((current) => images.length ? (current + direction + images.length) % images.length : 0);

  return <article className={`group min-w-0 overflow-hidden rounded-2xl border border-ink/10 bg-white ${outOfStock ? "opacity-85" : ""}`}>
    <div className="relative aspect-[4/5] overflow-hidden bg-mist">
      <Link href={`/products/${encodeURIComponent(product.slug)}`} className="block h-full"><CatalogImage src={image} alt={`${product.name} product image ${imageIndex + 1}`} sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw" className="transition duration-500 group-hover:scale-105" /><span className="absolute right-3 top-3 grid h-9 w-9 place-items-center rounded-full bg-white/90 text-ink shadow-sm"><ArrowUpRight className="h-4 w-4" /></span></Link>
      {outOfStock ? <span className="absolute left-3 top-3"><Badge tone="danger">Out of stock</Badge></span> : null}
      {images.length > 1 ? <><button type="button" aria-label="Previous product image" onClick={() => moveImage(-1)} className="absolute left-2 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-full bg-white/90 text-ink opacity-0 shadow-sm transition group-hover:opacity-100 focus:opacity-100"><ChevronLeft className="h-4 w-4" /></button><button type="button" aria-label="Next product image" onClick={() => moveImage(1)} className="absolute right-2 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-full bg-white/90 text-ink opacity-0 shadow-sm transition group-hover:opacity-100 focus:opacity-100"><ChevronRight className="h-4 w-4" /></button><div className="absolute bottom-3 left-1/2 flex -translate-x-1/2 gap-1 rounded-full bg-white/85 px-2 py-1" aria-label="Product images">{images.map((item, index) => <button key={item.id} type="button" aria-label={`View product image ${index + 1}`} aria-pressed={imageIndex === index} onClick={() => setImageIndex(index)} className={`h-1.5 rounded-full transition ${imageIndex === index ? "w-4 bg-ink" : "w-1.5 bg-ink/30"}`} />)}</div></> : null}
    </div>
    <Link href={`/products/${encodeURIComponent(product.slug)}`} className="block p-3.5 sm:p-4"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><h3 className="line-clamp-2 min-h-10 font-semibold leading-5 text-ink">{product.name}</h3>{variant?.attributes && Object.keys(variant.attributes).length ? <p className="mt-1 line-clamp-1 text-xs text-ink/50">{variantLabel(variant.attributes)}</p> : null}</div>{variant ? <p className="shrink-0 text-sm font-bold text-ink">{formatCurrency(lowestPrice, variant.currency)}</p> : null}</div><ReviewRating className="mt-3" summary={reviewSummary.data} isLoading={reviewSummary.isLoading} /><div className="mt-3"><Badge tone={outOfStock ? "danger" : inStock ? "success" : "neutral"}>{outOfStock ? "Out of stock" : checkingAvailability ? "Checking availability" : inStock ? "In stock" : "Availability unavailable"}</Badge></div></Link>
  </article>;
}

export function ProductGrid({ products }: { products: Product[] }) {
  return <div className="grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-3 lg:grid-cols-4">{products.map((product) => <ProductCard key={product.id} product={product} />)}</div>;
}
