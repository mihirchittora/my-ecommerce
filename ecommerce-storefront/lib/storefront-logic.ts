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

export function variantAttributeGroups(variants: ProductVariant[]) {
  const groups: Record<string, string[]> = {};
  variants.forEach((variant) => {
    Object.entries(variant.attributes).forEach(([key, value]) => {
      groups[key] ??= [];
      if (!groups[key].includes(value)) groups[key].push(value);
    });
  });
  return groups;
}

export function variantForAttributeSelection(variants: ProductVariant[], selection: Record<string, string>) {
  return variants.find((variant) => variant.status === "ACTIVE" && Object.entries(selection).every(([key, value]) => variant.attributes[key] === value)) ?? null;
}

export function attributeValueHasVariant(variants: ProductVariant[], selection: Record<string, string>, key: string, value: string) {
  return variants.some((variant) => {
    if (variant.status !== "ACTIVE" || variant.attributes[key] !== value) return false;
    return Object.entries(selection).every(([selectedKey, selectedValue]) => selectedKey === key || variant.attributes[selectedKey] === selectedValue);
  });
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
  sort: "name,asc" | "name,desc" | "brand,asc" | "createdAt,desc" | "price,asc" | "price,desc";
}

export function parseDiscoveryState(params: Pick<URLSearchParams, "get" | "getAll">, initialCategoryId?: string): DiscoveryState {
  const sort = params.get("sort");
  const rawPriceMin = params.get("priceMin");
  const rawPriceMax = params.get("priceMax");
  const priceMin = rawPriceMin === null || rawPriceMin.trim() === "" ? undefined : Number(rawPriceMin);
  const priceMax = rawPriceMax === null || rawPriceMax.trim() === "" ? undefined : Number(rawPriceMax);
  return {
    page: Math.max(0, Number(params.get("page") ?? "0") || 0),
    search: params.get("q") ?? params.get("search") ?? "",
    categoryId: params.get("category") ?? initialCategoryId,
    availability: params.get("availability") === "in_stock" ? "in_stock" : "all",
    priceMin: priceMin !== undefined && Number.isFinite(priceMin) ? priceMin : undefined,
    priceMax: priceMax !== undefined && Number.isFinite(priceMax) ? priceMax : undefined,
    brands: params.getAll("brand"),
    attributes: params.getAll("attribute"),
    sort: sort === "name,desc" || sort === "brand,asc" || sort === "createdAt,desc" || sort === "price,asc" || sort === "price,desc" ? sort : "name,asc",
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
