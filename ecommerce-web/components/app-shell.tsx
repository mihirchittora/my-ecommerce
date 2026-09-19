"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { BarChart3, ChevronRight, LayoutDashboard, Menu, Package, Shapes, Sparkles, UserRound } from "lucide-react";
import * as React from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";

const navItems = [{ href: "/", label: "Dashboard", icon: LayoutDashboard }, { href: "/categories", label: "Categories", icon: Shapes }, { href: "/products", label: "Products", icon: Package }];

function NavLinks({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();
  return <nav className="space-y-1" aria-label="Main navigation">{navItems.map((item) => { const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href); const Icon = item.icon; return <Link key={item.href} href={item.href} onClick={onNavigate} className={cn("group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors", active ? "bg-blue-50 text-primary" : "text-slate-500 hover:bg-slate-50 hover:text-slate-900")}><Icon className={cn("h-[18px] w-[18px]", active ? "text-primary" : "text-slate-400 group-hover:text-slate-600")} /><span>{item.label}</span>{active && <ChevronRight className="ml-auto h-4 w-4" />}</Link>; })}</nav>;
}

function Sidebar({ mobile = false, onNavigate }: { mobile?: boolean; onNavigate?: () => void }) {
  return <aside className={cn("flex h-full flex-col bg-white", mobile ? "w-full" : "border-r border-slate-200/80")}><div className="flex h-20 items-center gap-3 px-6"><div className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-950 text-white shadow-sm"><Sparkles className="h-4 w-4" /></div><div><p className="text-sm font-bold tracking-tight text-slate-950">Meridian</p><p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-slate-400">Catalog ops</p></div></div><div className="px-4 pt-5"><p className="mb-3 px-3 text-[10px] font-semibold uppercase tracking-[0.18em] text-slate-400">Workspace</p><NavLinks onNavigate={onNavigate} /></div><div className="mt-auto p-4"><div className="rounded-2xl bg-slate-950 p-4 text-white"><div className="mb-3 flex items-center justify-between"><BarChart3 className="h-5 w-5 text-blue-300" /><span className="text-[10px] font-semibold uppercase tracking-[0.15em] text-slate-400">Live</span></div><p className="text-sm font-semibold">Catalog health</p><p className="mt-1 text-xs leading-5 text-slate-400">Keep products, variants and imagery ready for launch.</p></div></div></aside>;
}

function Breadcrumbs() {
  const pathname = usePathname();
  const segments = pathname.split("/").filter(Boolean);
  const labels: Record<string, string> = { categories: "Categories", products: "Products", new: "New product" };
  return <div className="flex items-center gap-2 text-xs font-medium text-slate-400"><Link href="/" className="hover:text-slate-700">Workspace</Link>{segments.map((segment, index) => <React.Fragment key={`${segment}-${index}`}><ChevronRight className="h-3.5 w-3.5" /><span className={index === segments.length - 1 ? "text-slate-700" : ""}>{labels[segment] ?? (segment.length > 8 ? "Product details" : segment)}</span></React.Fragment>)}</div>;
}

export function AppShell({ children }: { children: React.ReactNode }) {
  return <div className="min-h-screen bg-background"><div className="hidden w-64 lg:fixed lg:inset-y-0 lg:flex"><Sidebar /></div><div className="lg:pl-64"><header className="sticky top-0 z-30 flex h-20 items-center justify-between border-b border-slate-200/80 bg-background/90 px-4 backdrop-blur-xl sm:px-6 lg:px-10"><div className="flex min-w-0 items-center gap-3"><div className="lg:hidden"><Dialog><DialogTrigger asChild><Button variant="outline" size="icon-sm" aria-label="Open navigation"><Menu className="h-4 w-4" /></Button></DialogTrigger><DialogContent className="left-0 top-0 h-full max-h-none w-72 -translate-x-0 -translate-y-0 rounded-none p-0"><DialogTitle className="sr-only">Navigation</DialogTitle><Sidebar mobile /></DialogContent></Dialog></div><Breadcrumbs /></div><div className="flex items-center gap-3"><div className="hidden text-right sm:block"><p className="text-xs font-semibold text-slate-700">Catalog manager</p><p className="text-[11px] text-slate-400">Operations workspace</p></div><DropdownMenu><DropdownMenuTrigger asChild><Button variant="outline" size="icon" className="rounded-full" aria-label="Open user menu"><UserRound className="h-4 w-4" /></Button></DropdownMenuTrigger><DropdownMenuContent align="end"><DropdownMenuItem>Profile settings</DropdownMenuItem><DropdownMenuItem>Keyboard shortcuts</DropdownMenuItem></DropdownMenuContent></DropdownMenu></div></header><main className="min-h-[calc(100vh-5rem)] px-4 py-6 sm:px-6 lg:px-10 lg:py-8">{children}</main></div></div>;
}
