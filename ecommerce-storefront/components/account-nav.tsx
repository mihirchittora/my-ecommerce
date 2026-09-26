"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

export function AccountNav() {
  const pathname = usePathname();
  return <nav aria-label="Account navigation" className="flex gap-2 overflow-x-auto pb-1"><Link href="/account" className={cn("whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold", pathname === "/account" ? "bg-ink text-white" : "bg-white text-ink/65 hover:text-ink")}>Overview</Link><Link href="/account/profile" className={cn("whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold", pathname === "/account/profile" ? "bg-ink text-white" : "bg-white text-ink/65 hover:text-ink")}>Profile</Link><Link href="/account/addresses" className={cn("whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold", pathname === "/account/addresses" ? "bg-ink text-white" : "bg-white text-ink/65 hover:text-ink")}>Addresses</Link><Link href="/account/wishlist" className={cn("whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold", pathname === "/account/wishlist" ? "bg-ink text-white" : "bg-white text-ink/65 hover:text-ink")}>Wishlist</Link><Link href="/orders" className={cn("whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold", pathname.startsWith("/orders") ? "bg-ink text-white" : "bg-white text-ink/65 hover:text-ink")}>Orders</Link><Link href="/account/returns" className={cn("whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold", pathname.startsWith("/account/returns") ? "bg-ink text-white" : "bg-white text-ink/65 hover:text-ink")}>Returns</Link></nav>;
}
