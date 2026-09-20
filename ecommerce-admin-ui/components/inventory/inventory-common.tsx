"use client";

import Link from "next/link";
import { AlertCircle, ChevronLeft, ChevronRight, RefreshCw } from "lucide-react";
import type { PageResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ApiError } from "@/lib/api/client";

export function InventoryServiceUnavailable({ error, onRetry }: { error?: unknown; onRetry?: () => void }) {
  const message = error instanceof ApiError ? error.message : "Inventory service temporarily unavailable.";
  return <Card className="flex flex-col items-center justify-center px-6 py-16 text-center"><div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-50 text-amber-600"><AlertCircle className="h-6 w-6" /></div><h2 className="text-base font-semibold text-slate-900">Inventory data is unavailable</h2><p className="mt-2 max-w-md text-sm leading-6 text-slate-500">{message} Catalog screens remain available while this service is offline.</p>{onRetry && <Button variant="outline" className="mt-5" onClick={onRetry}><RefreshCw className="h-4 w-4" />Retry</Button>}</Card>;
}

export function InventoryPagination<T>({ page, onPageChange }: { page: PageResponse<T>; onPageChange: (page: number) => void }) {
  return <div className="flex flex-col gap-3 border-t border-slate-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between"><p className="text-xs text-slate-500">Showing <span className="font-semibold text-slate-700">{page.numberOfElements === 0 ? 0 : page.number * page.size + 1}–{page.number * page.size + page.numberOfElements}</span> of <span className="font-semibold text-slate-700">{page.totalElements}</span></p><div className="flex items-center gap-2"><Button variant="outline" size="sm" onClick={() => onPageChange(page.number - 1)} disabled={page.first}><ChevronLeft className="h-4 w-4" />Previous</Button><span className="px-2 text-xs font-medium text-slate-500">Page {page.number + 1} of {Math.max(page.totalPages, 1)}</span><Button variant="outline" size="sm" onClick={() => onPageChange(page.number + 1)} disabled={page.last}>Next<ChevronRight className="h-4 w-4" /></Button></div></div>;
}

export function SkuLink({ sku, href = "/inventory/stock" }: { sku: string; href?: string }) { return <Link href={`${href}/${encodeURIComponent(sku)}`} className="font-mono text-sm font-semibold text-primary hover:underline">{sku}</Link>; }
