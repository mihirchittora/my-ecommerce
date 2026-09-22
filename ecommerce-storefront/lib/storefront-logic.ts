import type { Availability, Product, ProductVariant } from "@/lib/types";

export function availabilityLabel(availability: Availability | undefined) {
  if (!availability) return "Availability checked at checkout";
  return availability.available ? availability.message || "In stock" : "Out of stock";
}

export function activeVariants(variants: ProductVariant[]) {
  return variants.filter((variant) => variant.status === "ACTIVE");
}

export function selectedVariant(variants: ProductVariant[], sku: string | undefined) {
  return variants.find((variant) => variant.status === "ACTIVE" && variant.sku === sku) ?? null;
}

export function variantIsUnavailable(variant: ProductVariant, availability?: Availability) {
  return variant.status !== "ACTIVE" || availability?.available === false;
}

export function productIsOutOfStock(product: Product, availability: Record<string, Availability | undefined>) {
  const sellableVariants = product.variants.filter((variant) => variant.status === "ACTIVE");
  return sellableVariants.length === 0 || sellableVariants.every((variant) => availability[variant.sku]?.available === false);
}

export function humanizeCatalogValue(value: string) {
  return value
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
    .replace(/(\d+)(G|GB|ML|L)\b/i, "$1 $2")
    .replace(/\bGb\b/g, "GB")
    .replace(/\bG\b/g, "g")
    .replace(/\bMl\b/g, "ml");
}

export function humanizeCatalogKey(value: string) {
  return humanizeCatalogValue(value);
}

export interface DiscoveryState {
  page: number;
  search: string;
  categoryId?: string;
  availability: "all" | "in_stock";
  priceMin?: number;
  priceMax?: number;
  brands: string[];
  attributes: string[];
  sort: "name,asc" | "name,desc" | "createdAt,desc";
}

export function parseDiscoveryState(params: Pick<URLSearchParams, "get" | "getAll">, initialCategoryId?: string): DiscoveryState {
  const sort = params.get("sort");
  const priceMin = Number(params.get("priceMin"));
  const priceMax = Number(params.get("priceMax"));
  return {
    page: Math.max(0, Number(params.get("page") ?? "0") || 0),
    search: params.get("q") ?? params.get("search") ?? "",
    categoryId: params.get("category") ?? initialCategoryId,
    availability: params.get("availability") === "in_stock" ? "in_stock" : "all",
    priceMin: Number.isFinite(priceMin) ? priceMin : undefined,
    priceMax: Number.isFinite(priceMax) ? priceMax : undefined,
    brands: params.getAll("brand"),
    attributes: params.getAll("attribute"),
    sort: sort === "name,desc" || sort === "createdAt,desc" ? sort : "name,asc",
  };
}

export function discoveryQueryValues(state: DiscoveryState) {
  return {
    page: state.page,
    search: state.search || undefined,
    categoryId: state.categoryId,
    sort: state.sort,
    priceMin: state.priceMin,
    priceMax: state.priceMax,
    brands: state.brands,
    attributes: state.attributes,
  };
}

export function wrapGalleryIndex(index: number, direction: 1 | -1, length: number) {
  if (length <= 0) return 0;
  return (index + direction + length) % length;
}
