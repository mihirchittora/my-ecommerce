"use client";

import Link from "next/link";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { customerApi } from "@/lib/api/customer";
import { catalogApi } from "@/lib/api/catalog";
import { CatalogImage } from "@/components/catalog-image";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { EmptyState, ErrorState, LoadingBlock } from "@/components/feedback";
import { formatCurrency, getAssetUrl } from "@/lib/utils";

export default function WishlistPage() {
  const queryClient = useQueryClient();
  const wishlist = useQuery({ queryKey: ["customer", "wishlist"], queryFn: customerApi.wishlist });
  const products = useQueries({ queries: (wishlist.data ?? []).map((item) => ({ queryKey: ["wishlist-product", item.productId], queryFn: () => catalogApi.getProductById(item.productId), staleTime: 60_000 })) });
  const remove = useMutation({ mutationFn: (id: string) => customerApi.removeWishlist(id), onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["customer", "wishlist"] }) });
  if (wishlist.isLoading) return <LoadingBlock label="Loading wishlist" />;
  if (wishlist.isError) return <ErrorState message="Your wishlist could not be loaded." retry={() => void wishlist.refetch()} />;
  if (!wishlist.data?.length) return <EmptyState title="Your wishlist is empty" message="Save pieces you want to come back to." action={<Button asLink="/products">Browse products</Button>} />;
  return <div className="grid gap-4 md:grid-cols-2">{wishlist.data.map((item, index) => { const product = products[index]?.data; const image = product?.images.slice().sort((a, b) => a.sortOrder - b.sortOrder)[0]; const variant = product?.variants.find((candidate) => candidate.sku === item.sku) ?? product?.variants.find((candidate) => candidate.status === "ACTIVE"); return <Card key={item.id} className="flex gap-4 p-4"><div className="h-24 w-24 shrink-0 overflow-hidden rounded-2xl bg-mist">{image ? <CatalogImage src={getAssetUrl(image.url)} alt={product?.name ?? "Wishlist item"} sizes="96px" /> : null}</div><div className="min-w-0 flex-1"><p className="font-semibold">{product?.name ?? `Product ${item.productId}`}</p><p className="mt-1 text-sm text-ink/55">{item.sku ?? variant?.sku ?? "Current variant"}</p>{variant ? <p className="mt-2 font-bold">{formatCurrency(variant.price, variant.currency)}</p> : null}<div className="mt-3 flex gap-3"><Link href={product ? `/products/${product.slug}` : "/products"} className="text-sm font-bold text-moss underline">View product</Link><button type="button" className="text-sm font-bold text-red-700" onClick={() => remove.mutate(item.id)}>Remove</button></div></div></Card>; })}</div>;
}
