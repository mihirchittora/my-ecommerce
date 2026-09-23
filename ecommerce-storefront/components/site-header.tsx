"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { ChevronDown, Menu, Search, ShoppingBag, UserRound, X } from "lucide-react";
import { useQueries, useQuery } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { cartApi } from "@/lib/api/cart";
import { catalogApi } from "@/lib/api/catalog";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";

export function SiteHeader() {
  const { user, status } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const searchRef = useRef<HTMLDivElement>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [searchFocused, setSearchFocused] = useState(false);
  const cart = useQuery({ queryKey: ["cart"], queryFn: cartApi.get, enabled: status === "authenticated" });
  const categories = useQuery({ queryKey: ["categories", "root"], queryFn: () => catalogApi.listCategories() });
  const searchSuggestions = useQuery({
    queryKey: ["header-search-suggestions", debouncedQuery],
    queryFn: () => catalogApi.listProducts({ search: debouncedQuery, page: 0, size: 6, sort: "name,asc" }),
    enabled: searchFocused && debouncedQuery.length >= 2,
    staleTime: 30_000,
  });
  const itemCount = cart.data?.itemCount ?? 0;

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQuery(query.trim()), 220);
    return () => window.clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    const handleOutsidePointer = (event: PointerEvent) => {
      if (!searchRef.current?.contains(event.target as Node)) setSearchFocused(false);
    };
    document.addEventListener("pointerdown", handleOutsidePointer);
    return () => document.removeEventListener("pointerdown", handleOutsidePointer);
  }, []);

  function submitSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const value = query.trim();
    router.push(value ? `/search?q=${encodeURIComponent(value)}` : "/products");
    setSearchFocused(false);
    setMenuOpen(false);
  }

  const accountHref = user ? "/account" : `/login?next=${encodeURIComponent(pathname)}`;
  const rootCategories = categories.data?.filter((category) => !category.parentId) ?? [];
  const childCategoryQueries = useQueries({ queries: rootCategories.map((category) => ({ queryKey: ["categories", category.id], queryFn: () => catalogApi.listCategories(category.id), staleTime: 60_000 })) });
  const childCategoriesByParent = new Map(rootCategories.map((category, index) => [category.id, childCategoryQueries[index]?.data ?? []]));
  const categorySuggestions = [...rootCategories, ...Array.from(childCategoriesByParent.values()).flat()]
    .filter((category, index, all) => category.name.toLowerCase().includes(debouncedQuery.toLowerCase()) && all.findIndex((item) => item.id === category.id) === index)
    .slice(0, 4);
  const productSuggestions = searchSuggestions.data?.content ?? [];
  const showSuggestions = searchFocused && query.trim().length >= 2;
  const isActive = (href: string) => href === "/" ? pathname === "/" : pathname === href || pathname.startsWith(`${href}/`);
  const navLink = (href: string, label: string, key?: string) => <Link key={key} href={href} onClick={() => setMenuOpen(false)} aria-current={isActive(href) ? "page" : undefined} className={cn("shrink-0 border-b-2 border-transparent px-1 py-3 text-sm font-semibold text-ink/65 transition hover:border-coral hover:text-ink", isActive(href) && "border-coral text-ink")}>{label}</Link>;
  const categoryNavLink = (category: { id: string; name: string; slug: string }, children: { id: string; name: string; slug: string }[]) => <div key={`desktop-category-${category.id}`} className="group relative shrink-0">
    <Link href={`/categories/${encodeURIComponent(category.slug)}`} onClick={() => setMenuOpen(false)} aria-current={isActive(`/categories/${encodeURIComponent(category.slug)}`) ? "page" : undefined} aria-haspopup={children.length ? "menu" : undefined} className={cn("inline-flex items-center gap-1 border-b-2 border-transparent px-1 py-3 text-sm font-semibold text-ink/65 transition hover:border-coral hover:text-ink", isActive(`/categories/${encodeURIComponent(category.slug)}`) && "border-coral text-ink")}>{category.name}{children.length ? <ChevronDown className="h-3.5 w-3.5" /> : null}</Link>
    {children.length ? <div className="invisible absolute left-0 top-full z-50 w-56 translate-y-1 rounded-2xl border border-ink/10 bg-white p-2 opacity-0 shadow-xl transition group-hover:visible group-hover:translate-y-0 group-hover:opacity-100 group-focus-within:visible group-focus-within:translate-y-0 group-focus-within:opacity-100" role="menu">
      {children.map((child) => <Link key={child.id} href={`/categories/${encodeURIComponent(child.slug)}`} onClick={() => setMenuOpen(false)} role="menuitem" className="block rounded-xl px-3 py-2.5 text-sm font-semibold text-ink/70 hover:bg-mist hover:text-ink">{child.name}</Link>)}
    </div> : null}
  </div>;

  return <header className="sticky top-0 z-40 border-b border-ink/10 bg-white/95 backdrop-blur">
    <div className="hidden border-b border-ink/10 bg-sand/80 sm:block"><div className="page-shell flex h-8 items-center justify-end gap-5 text-[11px] font-semibold text-ink/55"><span>Thoughtful goods, clearly priced</span><Link href="/search" className="hover:text-ink">Help & search</Link><Link href={accountHref} className="hover:text-ink">{user ? "My account" : "Sign in"}</Link></div></div>
    <div className="page-shell flex items-center gap-3 py-3 md:gap-5 md:py-4">
      <button aria-label={menuOpen ? "Close menu" : "Open menu"} className="rounded-full p-2 text-ink hover:bg-mist md:hidden" onClick={() => setMenuOpen((open) => !open)}>{menuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}</button>
      <Link href="/" className="flex shrink-0 items-center gap-2" onClick={() => setMenuOpen(false)}><span className="grid h-9 w-9 place-items-center rounded-xl bg-ink text-sm font-black text-white">M</span><span className="font-display text-lg font-bold tracking-tight text-ink">Morrow</span></Link>
      <form onSubmit={submitSearch} className="order-3 w-full md:order-none md:mx-auto md:block md:max-w-2xl md:flex-1"><label className="sr-only" htmlFor="site-search">Search products</label><div ref={searchRef} className="relative"><Search className="pointer-events-none absolute left-4 top-1/2 z-10 h-4 w-4 -translate-y-1/2 text-ink/45" /><Input id="site-search" value={query} onChange={(event) => setQuery(event.target.value)} onFocus={() => setSearchFocused(true)} onKeyDown={(event) => { if (event.key === "Escape") setSearchFocused(false); }} aria-expanded={showSuggestions} aria-controls="header-search-suggestions" autoComplete="off" placeholder="Search products, brands, and more" className="h-11 rounded-full bg-sand/70 pl-11" />
        {showSuggestions ? <div id="header-search-suggestions" role="listbox" className="absolute left-0 right-0 top-full z-50 mt-2 overflow-hidden rounded-2xl border border-ink/10 bg-white p-2 shadow-xl">
          {searchSuggestions.isLoading ? <p className="px-3 py-4 text-sm text-ink/55">Finding matches…</p> : null}
          {!searchSuggestions.isLoading && categorySuggestions.length ? <div className="border-b border-ink/10 pb-2"><p className="px-3 py-2 text-[10px] font-bold uppercase tracking-[0.16em] text-moss">Categories</p>{categorySuggestions.map((category) => <Link key={`suggested-category-${category.id}`} href={`/categories/${encodeURIComponent(category.slug)}`} role="option" onClick={() => setSearchFocused(false)} className="block rounded-xl px-3 py-2 text-sm font-semibold text-ink/75 hover:bg-mist hover:text-ink">{category.name}</Link>)}</div> : null}
          {!searchSuggestions.isLoading && productSuggestions.length ? <div className="pt-2"><p className="px-3 py-2 text-[10px] font-bold uppercase tracking-[0.16em] text-moss">Products</p>{productSuggestions.map((product) => <Link key={product.id} href={`/products/${encodeURIComponent(product.slug)}`} role="option" onClick={() => setSearchFocused(false)} className="flex items-center justify-between gap-3 rounded-xl px-3 py-2.5 hover:bg-mist"><span className="min-w-0 truncate text-sm font-semibold text-ink">{product.name}</span><span className="shrink-0 text-xs text-ink/45">{product.brand ?? "Product"}</span></Link>)}</div> : null}
          {!searchSuggestions.isLoading && !categorySuggestions.length && !productSuggestions.length ? <p className="px-3 py-4 text-sm text-ink/55">No matches found.</p> : null}
          {!searchSuggestions.isLoading ? <Link href={`/search?q=${encodeURIComponent(query.trim())}`} onClick={() => setSearchFocused(false)} className="mt-2 block border-t border-ink/10 px-3 py-3 text-sm font-bold text-moss hover:text-ink">View all results for “{query.trim()}”</Link> : null}
        </div> : null}
      </div></form>
      <div className="ml-auto flex shrink-0 items-center gap-1"><Link className="rounded-full p-2.5 text-ink hover:bg-mist" href={accountHref} aria-label={user ? "Account" : "Sign in"}><UserRound className="h-5 w-5" /></Link><Link className="relative rounded-full p-2.5 text-ink hover:bg-mist" href="/cart" aria-label={`Cart${itemCount ? `, ${itemCount} items` : ""}`}><ShoppingBag className="h-5 w-5" />{itemCount ? <span className="absolute -right-0.5 -top-0.5 grid min-h-5 min-w-5 place-items-center rounded-full bg-coral px-1 text-[10px] font-bold text-white">{itemCount > 99 ? "99+" : itemCount}</span> : null}</Link></div>
    </div>
    <nav aria-label="Primary" className="border-t border-ink/10"><div className="page-shell relative flex items-center gap-5 overflow-x-auto whitespace-nowrap scrollbar-none md:overflow-visible">{navLink("/", "Home")}{rootCategories.slice(0, 6).map((category) => categoryNavLink(category, childCategoriesByParent.get(category.id) ?? []))}{navLink("/products", "Shop all")}{rootCategories.length > 6 ? <Link href="/categories" className="inline-flex shrink-0 items-center gap-1 py-3 text-sm font-semibold text-ink/65 hover:text-ink">More <ChevronDown className="h-3.5 w-3.5" /></Link> : navLink("/categories", "Categories")}</div></nav>
    <div className={cn("border-t border-ink/10 bg-sand px-5 pb-5 pt-4 md:hidden", menuOpen ? "block" : "hidden")}><nav className="grid gap-1 text-sm font-semibold"><p className="px-3 pb-2 text-xs font-bold uppercase tracking-[0.16em] text-moss">Browse</p>{navLink("/", "Home")}{rootCategories.map((category) => <div key={`mobile-category-${category.id}`}>{navLink(`/categories/${encodeURIComponent(category.slug)}`, category.name, `mobile-${category.id}`)}{(childCategoriesByParent.get(category.id) ?? []).length ? <div className="ml-5 grid gap-1 border-l border-ink/10 pl-3">{(childCategoriesByParent.get(category.id) ?? []).map((child) => navLink(`/categories/${encodeURIComponent(child.slug)}`, child.name, `mobile-child-${child.id}`))}</div> : null}</div>)}{navLink("/products", "Shop all")}{navLink("/categories", "All categories")}</nav><div className="mt-4 border-t border-ink/10 pt-4">{navLink(accountHref, user ? "My account" : "Sign in")}</div></div>
  </header>;
}
