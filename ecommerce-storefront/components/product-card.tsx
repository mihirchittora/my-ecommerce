"use client";

import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import { useQueries } from "@tanstack/react-query";
import type { Product } from "@/lib/types";
import { inventoryApi } from "@/lib/api/inventory";
import { formatCurrency, getAssetUrl, variantLabel } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { CatalogImage } from "@/components/catalog-image";
import { productIsOutOfStock } from "@/lib/storefront-logic";

export function ProductCard({ product }: { product: Product }) {
  const variants = product.variants.filter((item) => item.status === "ACTIVE");
  const variant = variants[0] ?? product.variants[0];
  const availabilityQueries = useQueries({ queries: variants.map((item) => ({ queryKey: ["availability", item.sku], queryFn: () => inventoryApi.getAvailability(item.sku), staleTime: 30_000 })) });
  const availability = Object.fromEntries(availabilityQueries.map((query, index) => [variants[index]?.sku, query.data]));
  const outOfStock = productIsOutOfStock(product, availability);
  const checkingAvailability = !outOfStock && availabilityQueries.some((query) => query.isPending);
  const inStock = availabilityQueries.some((query) => query.data?.available === true);
  const image = getAssetUrl([...product.images].sort((a, b) => a.sortOrder - b.sortOrder)[0]?.url);
  return <article className={`group min-w-0 ${outOfStock ? "opacity-85" : ""}`}><Link href={`/products/${encodeURIComponent(product.slug)}`} className="block"><div className="relative aspect-[4/5] overflow-hidden rounded-3xl bg-mist"><CatalogImage src={image} alt={product.name} sizes="(max-width: 768px) 50vw, (max-width: 1200px) 33vw, 25vw" className="transition duration-500 group-hover:scale-105" /><span className="absolute right-3 top-3 grid h-9 w-9 place-items-center rounded-full bg-white/90 text-ink shadow-sm"><ArrowUpRight className="h-4 w-4" /></span>{outOfStock ? <span className="absolute left-3 top-3"><Badge tone="danger">Out of stock</Badge></span> : null}</div><div className="mt-4 flex items-start justify-between gap-3"><div className="min-w-0"><h3 className="truncate font-semibold text-ink">{product.name}</h3>{variant?.attributes && Object.keys(variant.attributes).length ? <p className="mt-1 truncate text-xs text-ink/50">{variantLabel(variant.attributes)}</p> : null}</div>{variant ? <p className="shrink-0 text-sm font-bold text-ink">{formatCurrency(variant.price, variant.currency)}</p> : null}</div><div className="mt-3"><Badge tone={outOfStock ? "danger" : inStock ? "success" : "neutral"}>{outOfStock ? "Out of stock" : checkingAvailability ? "Checking availability" : inStock ? "In stock" : "Availability unavailable"}</Badge></div></Link></article>;
}

export function ProductGrid({ products }: { products: Product[] }) {
  return <div className="grid grid-cols-2 gap-x-4 gap-y-8 md:grid-cols-3 lg:grid-cols-4">{products.map((product) => <ProductCard key={product.id} product={product} />)}</div>;
}
