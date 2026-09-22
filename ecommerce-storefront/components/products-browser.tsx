"use client";

import { ChevronLeft, ChevronRight, Search, SlidersHorizontal, X } from "lucide-react";
import { useQueries, useQuery } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { catalogApi } from "@/lib/api/catalog";
import { inventoryApi } from "@/lib/api/inventory";
import { ProductGrid } from "@/components/product-card";
import { EmptyState, ErrorState, ProductSkeletonGrid } from "@/components/feedback";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { productIsOutOfStock, parseDiscoveryState } from "@/lib/storefront-logic";
import { formatCurrency, humanizeCatalogValue } from "@/lib/utils";

type QueryValue = string | string[] | undefined;

function FilterPanel({
  state,
  facets,
  categories,
  onUpdate,
  onClear,
}: {
  state: ReturnType<typeof parseDiscoveryState>;
  facets?: { price: { min: number | null; max: number | null }; brands: string[]; attributes: Record<string, string[]> };
  categories?: { id: string; name: string }[];
  onUpdate: (values: Record<string, QueryValue>) => void;
  onClear: () => void;
}) {
  const minimum = facets?.price.min ?? 0;
  const maximum = facets?.price.max ?? Math.max(minimum, 1000);
  const selectedMin = state.priceMin ?? minimum;
  const selectedMax = state.priceMax ?? maximum;
  const toggle = (key: "brand" | "attribute", value: string, selected: string[]) => onUpdate({ [key]: selected.includes(value) ? selected.filter((item) => item !== value) : [...selected, value] });
  return <div className="grid gap-7"><div className="flex items-center justify-between"><p className="font-display text-xl font-bold">Filter by</p><button type="button" className="text-xs font-bold text-moss hover:text-ink" onClick={onClear}>Clear all</button></div><fieldset><legend className="text-sm font-bold">Availability</legend><label className="mt-3 flex items-start gap-3 text-sm"><input type="checkbox" className="mt-0.5 h-4 w-4 accent-moss" checked={state.availability === "in_stock"} onChange={(event) => onUpdate({ availability: event.target.checked ? "in_stock" : undefined })} /><span><span className="font-semibold">In stock only</span><span className="mt-1 block text-xs leading-5 text-ink/50">Inventory does not yet expose a server-side catalog filter; this preference is kept in the URL and applied to the loaded page.</span></span></label></fieldset>{categories?.length ? <label className="grid gap-2"><span className="text-sm font-bold">Category</span><select aria-label="Filter by category" value={state.categoryId ?? ""} onChange={(event) => onUpdate({ category: event.target.value || undefined })} className="h-11 rounded-2xl border border-ink/15 bg-white px-3 text-sm"><option value="">All categories</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label> : null}{facets?.price.min !== null && facets?.price.max !== null ? <fieldset><legend className="text-sm font-bold">Price range</legend><div className="mt-3 grid gap-3"><div className="flex items-center justify-between text-xs text-ink/55"><span>{formatCurrency(selectedMin, "INR")}</span><span>{formatCurrency(selectedMax, "INR")}</span></div><input aria-label="Minimum price" type="range" min={minimum} max={maximum} step="50" value={selectedMin} onChange={(event) => onUpdate({ priceMin: String(Math.min(Number(event.target.value), selectedMax)) })} className="w-full accent-moss" /><input aria-label="Maximum price" type="range" min={minimum} max={maximum} step="50" value={selectedMax} onChange={(event) => onUpdate({ priceMax: String(Math.max(Number(event.target.value), selectedMin)) })} className="w-full accent-moss" /></div></fieldset> : null}{facets?.brands.length ? <fieldset><legend className="text-sm font-bold">Brand</legend><div className="mt-3 grid gap-2">{facets.brands.map((brand) => <label key={brand} className="flex items-center gap-3 text-sm"><input type="checkbox" className="h-4 w-4 accent-moss" checked={state.brands.includes(brand)} onChange={() => toggle("brand", brand, state.brands)} /><span>{brand}</span></label>)}</div></fieldset> : null}{Object.entries(facets?.attributes ?? {}).map(([key, values]) => <fieldset key={key}><legend className="text-sm font-bold">{humanizeCatalogValue(key)}</legend><div className="mt-3 grid gap-2">{values.map((value) => { const encoded = `${key}:${value}`; return <label key={encoded} className="flex items-center gap-3 text-sm"><input type="checkbox" className="h-4 w-4 accent-moss" checked={state.attributes.includes(encoded)} onChange={() => toggle("attribute", encoded, state.attributes)} /><span>{humanizeCatalogValue(value)}</span></label>; })}</div></fieldset>)}</div>;
}

export function ProductsBrowser({ searchOnly = false, initialCategoryId, categoryHeading }: { searchOnly?: boolean; initialCategoryId?: string; categoryHeading?: string }) {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const state = parseDiscoveryState(params, initialCategoryId);
  const [search, setSearch] = useState(state.search);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const categories = useQuery({ queryKey: ["categories", "root"], queryFn: () => catalogApi.listCategories(), enabled: !searchOnly });
  const facets = useQuery({ queryKey: ["product-facets", { search: state.search, categoryId: state.categoryId }], queryFn: () => catalogApi.getFacets({ search: state.search || undefined, categoryId: state.categoryId }), enabled: !searchOnly || Boolean(state.search) });
  const products = useQuery({ queryKey: ["products", state], queryFn: () => catalogApi.listProducts({ page: state.page, search: state.search || undefined, categoryId: state.categoryId, sort: state.sort, priceMin: state.priceMin, priceMax: state.priceMax, brands: state.brands, attributes: state.attributes }) });
  const activeSkus = useMemo(() => products.data?.content.flatMap((product) => product.variants.filter((variant) => variant.status === "ACTIVE").map((variant) => variant.sku)) ?? [], [products.data?.content]);
  const availabilityQueries = useQueries({ queries: activeSkus.map((sku) => ({ queryKey: ["availability", sku], queryFn: () => inventoryApi.getAvailability(sku), staleTime: 30_000 })) });
  const availabilityBySku = Object.fromEntries(availabilityQueries.map((query, index) => [activeSkus[index], query.data]));
  const visibleProducts = state.availability === "in_stock" ? products.data?.content.filter((product) => !productIsOutOfStock(product, availabilityBySku)) ?? [] : products.data?.content ?? [];

  useEffect(() => setSearch(state.search), [state.search]);

  function update(values: Record<string, QueryValue>) {
    const next = new URLSearchParams(params.toString());
    Object.entries(values).forEach(([key, value]) => { next.delete(key); if (Array.isArray(value)) value.forEach((item) => next.append(key, item)); else if (value) next.set(key, value); });
    if (Object.keys(values).some((key) => ["search", "q", "category", "availability", "priceMin", "priceMax", "brand", "attribute", "sort"].includes(key))) next.delete("page");
    router.push(`${pathname}${next.toString() ? `?${next.toString()}` : ""}`);
  }

  function clearFilters() {
    update({ category: undefined, availability: undefined, priceMin: undefined, priceMax: undefined, brand: undefined, attribute: undefined });
  }

  function submitSearch(event: React.FormEvent<HTMLFormElement>) { event.preventDefault(); update({ [searchOnly ? "q" : "search"]: search.trim() || undefined }); }

  const activeChips = [
    ...(state.categoryId && categories.data?.some((category) => category.id === state.categoryId) ? [{ key: "category", label: categories.data.find((category) => category.id === state.categoryId)?.name ?? "Category", remove: () => update({ category: undefined }) }] : []),
    ...state.brands.map((brand) => ({ key: `brand:${brand}`, label: brand, remove: () => update({ brand: state.brands.filter((item) => item !== brand) }) })),
    ...state.attributes.map((attribute) => ({ key: `attribute:${attribute}`, label: humanizeCatalogValue(attribute.replace(":", " · ")), remove: () => update({ attribute: state.attributes.filter((item) => item !== attribute) }) })),
    ...(state.availability === "in_stock" ? [{ key: "availability", label: "In stock", remove: () => update({ availability: undefined }) }] : []),
    ...(state.priceMin !== undefined || state.priceMax !== undefined ? [{ key: "price", label: `${formatCurrency(state.priceMin, "INR")}–${formatCurrency(state.priceMax, "INR")}`, remove: () => update({ priceMin: undefined, priceMax: undefined }) }] : []),
  ];
  const heading = categoryHeading ?? (searchOnly ? (state.search ? `Results for “${state.search}”` : "Search the collection") : "Shop all products");
  const countLabel = state.availability === "in_stock" ? `${visibleProducts.length} products shown on this page` : `${products.data?.totalElements ?? 0} products`;
  return <div className="page-shell py-12 md:py-16"><div className="max-w-2xl"><p className="eyebrow">{searchOnly ? "Search" : "The collection"}</p><h1 className="mt-3 font-display text-4xl font-bold tracking-tight md:text-6xl">{heading}</h1><p className="mt-5 text-base leading-7 text-ink/60">Browse the current catalogue with filters that reflect its live attributes.</p></div><div className="mt-9 flex flex-col gap-3 rounded-3xl bg-white p-4 shadow-soft md:flex-row"><form onSubmit={submitSearch} className="relative flex-1"><label className="sr-only" htmlFor="browse-search">Search products</label><Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-ink/40" /><Input id="browse-search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search by product name" className="pl-11" /></form><div className="flex gap-3 md:contents"><Button type="button" variant="secondary" className="md:hidden" onClick={() => setFiltersOpen(true)}><SlidersHorizontal className="mr-2 h-4 w-4" />Filter</Button><label className="flex min-w-44 flex-1 items-center md:flex-none"><span className="sr-only">Sort products</span><select aria-label="Sort products" value={state.sort} onChange={(event) => update({ sort: event.target.value })} className="h-12 w-full rounded-2xl border border-ink/15 bg-white px-4 text-sm"><option value="name,asc">Name: A–Z</option><option value="name,desc">Name: Z–A</option><option value="createdAt,desc">Newest</option></select></label><Button type="submit" className="hidden md:inline-flex">Search</Button></div></div><div className="mt-7 flex flex-wrap items-center justify-between gap-3"><p className="text-sm font-semibold text-ink/65" aria-live="polite">{products.isLoading ? "Loading products…" : countLabel}</p>{activeChips.length ? <button type="button" className="text-sm font-bold text-moss hover:text-ink" onClick={clearFilters}>Clear all</button> : null}</div>{activeChips.length ? <div className="mt-3 flex flex-wrap gap-2">{activeChips.map((chip) => <button key={chip.key} type="button" onClick={chip.remove} className="inline-flex items-center gap-1 rounded-full bg-mist px-3 py-1.5 text-xs font-semibold text-moss">{chip.label}<X className="h-3.5 w-3.5" /></button>)}</div> : null}<div className="mt-8 grid gap-10 lg:grid-cols-[240px_1fr]"><aside className="hidden lg:block"><FilterPanel state={state} facets={facets.data} categories={categories.data} onUpdate={update} onClear={clearFilters} /></aside>{filtersOpen ? <div className="fixed inset-0 z-50 bg-ink/30 lg:hidden" role="presentation" onClick={() => setFiltersOpen(false)}><aside className="absolute inset-y-0 left-0 w-[min(88vw,360px)] overflow-y-auto bg-sand p-6 shadow-xl" role="dialog" aria-modal="true" aria-label="Filters" onClick={(event) => event.stopPropagation()}><div className="mb-6 flex justify-end"><button type="button" aria-label="Close filters" onClick={() => setFiltersOpen(false)} className="rounded-full p-2 hover:bg-mist"><X className="h-5 w-5" /></button></div><FilterPanel state={state} facets={facets.data} categories={categories.data} onUpdate={update} onClear={() => { clearFilters(); setFiltersOpen(false); }} /></aside></div> : null}<div>{products.isLoading ? <ProductSkeletonGrid /> : products.isError ? <ErrorState message="Products are temporarily unavailable." retry={() => void products.refetch()} /> : visibleProducts.length ? <ProductGrid products={visibleProducts} /> : <EmptyState title="No products found" message={state.availability === "in_stock" ? "Unavailable products are hidden for this view. Clear the availability filter to see the full catalogue." : "Try another search or browse all categories."} action={<Button variant="secondary" onClick={clearFilters}>Clear filters</Button>} />}</div></div>{products.data && products.data.totalPages > 1 ? <div className="mt-12 flex items-center justify-between border-t border-ink/10 pt-5"><p className="text-sm text-ink/55">Page {products.data.number + 1} of {products.data.totalPages}</p><div className="flex gap-2"><Button variant="secondary" disabled={products.data.first} onClick={() => update({ page: String(state.page - 1) })}><ChevronLeft className="mr-1 h-4 w-4" />Previous</Button><Button variant="secondary" disabled={products.data.last} onClick={() => update({ page: String(state.page + 1) })}>Next<ChevronRight className="ml-1 h-4 w-4" /></Button></div></div> : null}</div>;
}
