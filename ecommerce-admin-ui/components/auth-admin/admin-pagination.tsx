import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { PageResponse } from "@/lib/types";

export function AdminPagination<T>({ page, onPageChange }: { page: PageResponse<T>; onPageChange: (page: number) => void }) {
  return <div className="flex flex-col gap-3 border-t border-slate-100 px-5 py-4 text-sm sm:flex-row sm:items-center sm:justify-between"><p className="text-slate-500">Page {page.number + 1} of {Math.max(page.totalPages, 1)} · {page.totalElements} record{page.totalElements === 1 ? "" : "s"}</p><div className="flex gap-2"><Button variant="outline" size="sm" disabled={page.first} onClick={() => onPageChange(page.number - 1)}><ChevronLeft className="h-4 w-4" />Previous</Button><Button variant="outline" size="sm" disabled={page.last} onClick={() => onPageChange(page.number + 1)}>Next<ChevronRight className="h-4 w-4" /></Button></div></div>;
}

