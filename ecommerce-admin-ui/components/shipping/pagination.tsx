import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { PageResponse } from "@/lib/types";

export function ShippingPagination<T>({ page, pageSize, onPageChange, onPageSizeChange }: { page: PageResponse<T>; pageSize: number; onPageChange: (page: number) => void; onPageSizeChange: (size: number) => void }) {
  return <div className="flex flex-col gap-3 border-t border-slate-100 px-5 py-4 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between"><span>Page {page.number + 1} of {Math.max(page.totalPages, 1)} · {page.totalElements} total</span><div className="flex items-center gap-3"><label className="flex items-center gap-2 text-xs font-medium">Rows<select value={pageSize} onChange={(event) => onPageSizeChange(Number(event.target.value))} className="h-9 rounded-lg border border-input bg-background px-2 text-sm"><option value="20">20</option><option value="50">50</option><option value="100">100</option></select></label><Button variant="outline" size="icon-sm" aria-label="Previous page" disabled={page.first} onClick={() => onPageChange(page.number - 1)}><ChevronLeft className="h-4 w-4" /></Button><Button variant="outline" size="icon-sm" aria-label="Next page" disabled={page.last} onClick={() => onPageChange(page.number + 1)}><ChevronRight className="h-4 w-4" /></Button></div></div>;
}
