"use client";

import Link from "next/link";
import { Check, ChevronLeft, ChevronRight, Minus, Plus, ShoppingBag, Truck } from "lucide-react";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { catalogApi } from "@/lib/api/catalog";
import { cartApi } from "@/lib/api/cart";
import { inventoryApi } from "@/lib/api/inventory";
import { useAuth } from "@/components/auth-provider";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ErrorState, LoadingBlock } from "@/components/feedback";
import { CatalogImage } from "@/components/catalog-image";
import { formatCurrency, getAssetUrl, variantLabel } from "@/lib/utils";
import { track } from "@/lib/analytics";
import { availabilityLabel, productIsOutOfStock, variantIsUnavailable, wrapGalleryIndex } from "@/lib/storefront-logic";

export function ProductDetail({ slug }: { slug: string }) {
  const { status } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();
  const product = useQuery({ queryKey: ["product", slug], queryFn: () => catalogApi.getProductBySlug(slug) });
  const [selectedSku, setSelectedSku] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [selectedImage, setSelectedImage] = useState(0);
  const [touchStart, setTouchStart] = useState<number | null>(null);
  const [added, setAdded] = useState(false);
  const variants = product.data?.variants ?? [];
  const activeVariants = variants.filter((variant) => variant.status === "ACTIVE");
  const availabilityQueries = useQueries({ queries: activeVariants.map((item) => ({ queryKey: ["availability", item.sku], queryFn: () => inventoryApi.getAvailability(item.sku), staleTime: 30_000 })) });
  const availabilityBySku = Object.fromEntries(availabilityQueries.map((query, index) => [activeVariants[index]?.sku, query.data]));
  const variant = variants.find((item) => item.sku === selectedSku && item.status === "ACTIVE") ?? null;
  const selectedAvailability = variant ? availabilityBySku[variant.sku] : undefined;
  const availabilityKnown = Boolean(variant && availabilityQueries.find((query, index) => activeVariants[index]?.sku === variant.sku)?.isFetched);
  const liveAvailability = selectedAvailability;
  const addToCart = useMutation({ mutationFn: () => cartApi.addItem(selectedSku, quantity), onSuccess: () => { setAdded(true); void queryClient.invalidateQueries({ queryKey: ["cart"] }); track("add_to_cart", { sku: selectedSku, quantity }); } });

  useEffect(() => { if (product.data) track("product_view", { product: product.data.slug }); }, [product.data]);
  useEffect(() => {
    if (!selectedSku && activeVariants.length) setSelectedSku(activeVariants[0].sku);
  }, [activeVariants, selectedSku]);
  useEffect(() => { setSelectedImage(0); }, [selectedSku]);

  if (product.isLoading) return <div className="page-shell py-16"><LoadingBlock label="Loading product" /></div>;
  if (product.isError || !product.data) return <div className="page-shell py-16"><ErrorState message="This product could not be loaded." retry={() => void product.refetch()} /></div>;

  const images = [...product.data.images].sort((a, b) => a.sortOrder - b.sortOrder);
  const variantImages = variant ? images.filter((image) => image.variantId === variant.id) : [];
  const galleryImages = variantImages.length ? variantImages : images.filter((image) => !image.variantId);
  const safeImageIndex = Math.min(selectedImage, Math.max(0, galleryImages.length - 1));
  const image = getAssetUrl(galleryImages[safeImageIndex]?.url);
  const productOutOfStock = productIsOutOfStock(product.data, availabilityBySku);
  const variantUnavailable = Boolean(variant && variantIsUnavailable(variant, liveAvailability));
  const canAdd = Boolean(product.data.status === "ACTIVE" && variant && liveAvailability?.available === true && !addToCart.isPending);
  const structuredData = JSON.stringify({ "@context": "https://schema.org", "@type": "Product", name: product.data.name, description: product.data.description ?? undefined, brand: product.data.brand ? { "@type": "Brand", name: product.data.brand } : undefined, image: images.map((item) => getAssetUrl(item.url)).filter((item): item is string => Boolean(item)), offers: variant ? { "@type": "Offer", priceCurrency: variant.currency, price: variant.price, availability: liveAvailability?.available === false ? "https://schema.org/OutOfStock" : "https://schema.org/InStock", url: `${process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3001"}/products/${product.data.slug}` } : undefined }).replace(/</g, "\\u003c");
  const moveImage = (direction: 1 | -1) => {
    if (galleryImages.length < 2) return;
    setSelectedImage((current) => wrapGalleryIndex(current, direction, galleryImages.length));
  };

  return <><div className="page-shell py-10 md:py-16"><div className="grid gap-10 lg:grid-cols-[1.1fr_0.9fr] lg:gap-16"><div><div className="relative aspect-square overflow-hidden rounded-[2rem] bg-mist" onTouchStart={(event) => setTouchStart(event.touches[0]?.clientX ?? null)} onTouchEnd={(event) => { if (touchStart === null) return; const delta = (event.changedTouches[0]?.clientX ?? touchStart) - touchStart; if (Math.abs(delta) > 40) moveImage(delta < 0 ? 1 : -1); setTouchStart(null); }}><CatalogImage src={image} alt={`${product.data.name} product image ${safeImageIndex + 1}`} fallbackLabel="Product image unavailable" priority sizes="(max-width: 1024px) 100vw, 55vw" />{galleryImages.length > 1 ? <><button type="button" aria-label="Previous product image" onClick={() => moveImage(-1)} className="absolute left-3 top-1/2 grid h-10 w-10 -translate-y-1/2 place-items-center rounded-full bg-white/90 text-ink shadow-sm"><ChevronLeft className="h-5 w-5" /></button><button type="button" aria-label="Next product image" onClick={() => moveImage(1)} className="absolute right-3 top-1/2 grid h-10 w-10 -translate-y-1/2 place-items-center rounded-full bg-white/90 text-ink shadow-sm"><ChevronRight className="h-5 w-5" /></button></> : null}</div>{galleryImages.length > 1 ? <div className="mt-4 flex min-w-0 gap-3 overflow-x-auto pb-2" aria-label="Product image thumbnails">{galleryImages.map((item, index) => { const thumb = getAssetUrl(item.url); return <button key={item.id} type="button" aria-label={`View product image ${index + 1}`} aria-pressed={safeImageIndex === index} onClick={() => setSelectedImage(index)} className={`relative h-20 w-20 shrink-0 overflow-hidden rounded-2xl bg-mist ${safeImageIndex === index ? "ring-2 ring-coral ring-offset-2" : "opacity-70 hover:opacity-100"}`}><CatalogImage src={thumb} alt="" sizes="80px" fallbackLabel="" /></button>; })}</div> : null}</div><div className="lg:pt-4"><div className="flex flex-wrap items-center gap-2"><Badge>{product.data.brand ?? "Morrow collection"}</Badge>{productOutOfStock ? <Badge tone="danger">Out of stock</Badge> : product.data.status === "ACTIVE" ? <Badge tone="success">Available to order</Badge> : <Badge tone="danger">Unavailable</Badge>}</div><h1 className="mt-5 font-display text-4xl font-bold tracking-tight md:text-6xl">{product.data.name}</h1>{product.data.description ? <p className="mt-6 whitespace-pre-line text-base leading-7 text-ink/65">{product.data.description}</p> : null}<div className="mt-8 border-y border-ink/10 py-6"><div className="flex items-end justify-between gap-4"><div>{variant ? <p className="text-3xl font-bold text-ink">{formatCurrency(variant.price, variant.currency)}</p> : <p className="text-lg font-semibold text-ink/50">Select a variant to see price</p>}{variant ? <p className="mt-2 text-xs text-ink/45">SKU {variant.sku}</p> : null}</div>{variant ? <Badge tone={variantUnavailable ? "danger" : liveAvailability?.available ? "success" : liveAvailability ? "danger" : "neutral"}>{variantUnavailable ? "Out of stock" : liveAvailability ? availabilityLabel(liveAvailability) : "Checking availability"}</Badge> : null}</div></div>{variants.length > 1 ? <fieldset className="mt-7"><legend className="text-sm font-bold">Choose a variant</legend><div className="mt-3 grid gap-3 sm:grid-cols-2">{variants.map((item) => { const itemAvailability = availabilityBySku[item.sku]; const unavailable = variantIsUnavailable(item, itemAvailability); return <button key={item.sku} type="button" disabled={unavailable} onClick={() => { setSelectedSku(item.sku); setAdded(false); }} aria-pressed={selectedSku === item.sku} aria-disabled={unavailable} className={`rounded-2xl border p-4 text-left transition disabled:cursor-not-allowed disabled:opacity-60 ${selectedSku === item.sku ? "border-moss bg-mist ring-2 ring-moss/20" : "border-ink/10 bg-white hover:border-moss/50"}`}><span className="flex items-center justify-between gap-3"><span className="font-semibold">{variantLabel(item.attributes) || item.sku}</span>{selectedSku === item.sku && !unavailable ? <Check className="h-4 w-4 text-moss" /> : null}</span><span className="mt-1 block text-sm text-ink/55">{formatCurrency(item.price, item.currency)}</span>{unavailable ? <span className="mt-2 block text-xs font-bold text-red-700">{item.status !== "ACTIVE" ? "Unavailable" : itemAvailability?.message || "Out of stock"}</span> : null}</button>; })}</div></fieldset> : null}<div className="mt-7 flex flex-col gap-3 sm:flex-row"><div className="flex h-12 items-center justify-between rounded-full border border-ink/15 bg-white px-2 sm:w-36"><button type="button" aria-label="Decrease quantity" className="grid h-9 w-9 place-items-center rounded-full hover:bg-mist" onClick={() => setQuantity((value) => Math.max(1, value - 1))}><Minus className="h-4 w-4" /></button><span className="text-sm font-bold">{quantity}</span><button type="button" aria-label="Increase quantity" className="grid h-9 w-9 place-items-center rounded-full hover:bg-mist" onClick={() => setQuantity((value) => Math.min(100, value + 1))}><Plus className="h-4 w-4" /></button></div><Button className="flex-1" disabled={!canAdd} onClick={() => { if (status !== "authenticated") { router.push(`/login?next=${encodeURIComponent(`/products/${slug}`)}`); return; } addToCart.mutate(); }}>{addToCart.isPending ? "Adding…" : added ? "Added to cart" : product.data.status !== "ACTIVE" ? "Unavailable" : variantUnavailable ? "Out of stock" : availabilityKnown ? "Add to cart" : "Checking availability"}<ShoppingBag className="ml-2 h-4 w-4" /></Button></div>{addToCart.isError ? <p role="alert" className="mt-3 text-sm text-red-700">Could not add this item. Please try again.</p> : null}{added ? <p className="mt-3 text-sm font-semibold text-moss">Added to your cart. <Link href="/cart" className="underline">View cart</Link></p> : null}<Card className="mt-8 bg-mist p-5 shadow-none"><div className="flex gap-3"><Truck className="mt-0.5 h-5 w-5 shrink-0 text-moss" /><div><p className="text-sm font-bold">Availability and delivery</p><p className="mt-1 text-sm leading-6 text-ink/60">Stock is checked live for your selected variant. Final availability is confirmed when your order is created.</p></div></div></Card></div></div></div><script type="application/ld+json" dangerouslySetInnerHTML={{ __html: structuredData }} /></>;
}
