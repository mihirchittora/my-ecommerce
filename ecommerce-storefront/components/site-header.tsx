"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Menu, Search, ShoppingBag, UserRound, X } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { cartApi } from "@/lib/api/cart";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";

export function SiteHeader() {
  const { user, status } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const [query, setQuery] = useState("");
  const cart = useQuery({ queryKey: ["cart"], queryFn: cartApi.get, enabled: status === "authenticated" });
  const itemCount = cart.data?.itemCount ?? 0;

  function submitSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const value = query.trim();
    router.push(value ? `/search?q=${encodeURIComponent(value)}` : "/products");
    setMenuOpen(false);
  }

  const accountHref = user ? "/account" : `/login?next=${encodeURIComponent(pathname)}`;
  return <header className="sticky top-0 z-40 border-b border-ink/10 bg-sand/95 backdrop-blur">
    <div className="mx-auto flex max-w-7xl items-center gap-4 px-5 py-4 lg:px-8">
      <button aria-label={menuOpen ? "Close menu" : "Open menu"} className="rounded-full p-2 text-ink hover:bg-white md:hidden" onClick={() => setMenuOpen((open) => !open)}>{menuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}</button>
      <Link href="/" className="flex shrink-0 items-center gap-2" onClick={() => setMenuOpen(false)}><span className="grid h-9 w-9 place-items-center rounded-2xl bg-ink text-sm font-black text-white">M</span><span className="font-display text-lg font-bold tracking-tight text-ink">Morrow</span></Link>
      <nav className="hidden items-center gap-5 text-sm font-semibold text-ink/70 md:flex"><Link className="hover:text-moss" href="/categories">Categories</Link><Link className="hover:text-moss" href="/products">Shop all</Link></nav>
      <form onSubmit={submitSearch} className="mx-auto hidden max-w-xl flex-1 md:block"><label className="sr-only" htmlFor="site-search">Search products</label><div className="relative"><Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-ink/45" /><Input id="site-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search products, brands, and more" className="h-11 rounded-full pl-11" /></div></form>
      <div className="ml-auto flex items-center gap-1"><Link className="rounded-full p-2.5 text-ink hover:bg-white" href={accountHref} aria-label={user ? "Account" : "Sign in"}><UserRound className="h-5 w-5" /></Link><Link className="relative rounded-full p-2.5 text-ink hover:bg-white" href="/cart" aria-label={`Cart${itemCount ? `, ${itemCount} items` : ""}`}><ShoppingBag className="h-5 w-5" />{itemCount ? <span className="absolute -right-0.5 -top-0.5 grid min-h-5 min-w-5 place-items-center rounded-full bg-coral px-1 text-[10px] font-bold text-white">{itemCount > 99 ? "99+" : itemCount}</span> : null}</Link></div>
    </div>
    <div className={cn("border-t border-ink/10 px-5 pb-4 pt-3 md:hidden", menuOpen ? "block" : "hidden")}><form onSubmit={submitSearch}><label className="sr-only" htmlFor="mobile-search">Search products</label><div className="relative"><Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-ink/45" /><Input id="mobile-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search products" className="h-11 rounded-full pl-11" /></div></form><nav className="mt-4 grid gap-2 text-sm font-semibold"><Link href="/categories" onClick={() => setMenuOpen(false)} className="rounded-xl px-3 py-2 hover:bg-white">Categories</Link><Link href="/products" onClick={() => setMenuOpen(false)} className="rounded-xl px-3 py-2 hover:bg-white">Shop all</Link><Link href={accountHref} onClick={() => setMenuOpen(false)} className="rounded-xl px-3 py-2 hover:bg-white">{user ? "My account" : "Sign in"}</Link></nav></div>
  </header>;
}
