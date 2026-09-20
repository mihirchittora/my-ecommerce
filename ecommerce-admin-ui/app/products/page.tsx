"use client";

import { ListFilter, Plus, Search } from "lucide-react";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";
import { ProductTable } from "@/components/products/product-table";
import { flattenCategoryOptions } from "@/components/products/category-options";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { useCategoryTree, useProductList } from "@/lib/queries";

export default function ProductsPage() {
  const [search, setSearch] = useState("");
  const [querySearch, setQuerySearch] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [page, setPage] = useState(0);
  const [sortField, setSortField] = useState<"name" | "brand" | "createdAt">("name");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("asc");
  useEffect(() => { const timer = window.setTimeout(() => { setQuerySearch(search.trim()); setPage(0); }, 350); return () => window.clearTimeout(timer); }, [search]);
  const categories = useCategoryTree();
  const products = useProductList({ page, size: 10, sort: `${sortField},${sortDirection}`, search: querySearch || undefined, categoryId: categoryId || undefined });
  const categoryOptions = useMemo(() => flattenCategoryOptions(categories.data ?? []), [categories.data]);
  const categoryNames = useMemo(() => Object.fromEntries(categoryOptions.map((option) => [option.id, option.name])), [categoryOptions]);
  const sort = (field: "name" | "brand" | "createdAt") => { if (sortField === field) setSortDirection((value) => value === "asc" ? "desc" : "asc"); else { setSortField(field); setSortDirection("asc"); } setPage(0); };
  return <><PageIntro eyebrow="Assortment" title="Products" description="Search, filter and maintain the products that make up your catalog." action={{ label: "Add product", href: "/products/new" }} /><Card><CardContent className="p-4"><div className="flex flex-col gap-3 lg:flex-row"><div className="relative flex-1"><Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><Input value={search} onChange={(event) => setSearch(event.target.value)} className="pl-9" placeholder="Search by product name…" aria-label="Search products" /></div><div className="flex flex-col gap-3 sm:flex-row"><div className="relative min-w-56"><ListFilter className="pointer-events-none absolute left-3 top-1/2 z-10 h-4 w-4 -translate-y-1/2 text-slate-400" /><Select value={categoryId} onChange={(event) => { setCategoryId(event.target.value); setPage(0); }} className="pl-9" aria-label="Filter by category"><option value="">All categories</option>{categoryOptions.map((option) => <option key={option.id} value={option.id}>{"— ".repeat(option.depth)}{option.name}</option>)}</Select></div><Button asChild variant="outline"><Link href="/products/new"><Plus className="h-4 w-4" />New product</Link></Button></div></div></CardContent></Card><div className="mt-5">{products.isLoading ? <LoadingCard rows={7} /> : products.isError ? <ErrorState onRetry={() => void products.refetch()} /> : products.data?.totalElements === 0 ? <EmptyState title="Your catalog is empty" message="Create a product to start building the assortment." action={<Button asChild><Link href="/products/new"><Plus className="h-4 w-4" />Create product</Link></Button>} /> : <Card className="overflow-hidden"><ProductTable page={products.data} categoryNames={categoryNames} onPageChange={setPage} onSort={sort} sortField={sortField} sortDirection={sortDirection} /></Card>}</div></>;
}
