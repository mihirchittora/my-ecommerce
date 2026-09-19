import { AlertCircle, Inbox, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";

export function ErrorState({ title = "We couldn’t load this view", message = "Check that the catalog service is running and try again.", onRetry }: { title?: string; message?: string; onRetry?: () => void }) {
  return <Card className="flex flex-col items-center justify-center px-6 py-16 text-center"><div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-rose-600"><AlertCircle className="h-6 w-6" /></div><h2 className="text-base font-semibold text-slate-900">{title}</h2><p className="mt-2 max-w-md text-sm leading-6 text-slate-500">{message}</p>{onRetry && <Button variant="outline" className="mt-5" onClick={onRetry}><RefreshCw className="h-4 w-4" />Try again</Button>}</Card>;
}

export function EmptyState({ title, message, action }: { title: string; message: string; action?: React.ReactNode }) {
  return <Card className="flex flex-col items-center justify-center px-6 py-16 text-center"><div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-500"><Inbox className="h-6 w-6" /></div><h2 className="text-base font-semibold text-slate-900">{title}</h2><p className="mt-2 max-w-md text-sm leading-6 text-slate-500">{message}</p>{action && <div className="mt-5">{action}</div>}</Card>;
}

export function LoadingCard({ rows = 5 }: { rows?: number }) {
  return <Card className="overflow-hidden"><div className="space-y-0">{Array.from({ length: rows }).map((_, index) => <div key={index} className="flex items-center gap-4 border-b border-slate-100 p-5"><div className="h-10 w-10 animate-pulse rounded-xl bg-slate-100" /><div className="flex-1 space-y-2"><div className="h-3 w-1/3 animate-pulse rounded bg-slate-100" /><div className="h-2.5 w-1/2 animate-pulse rounded bg-slate-100" /></div><div className="h-8 w-20 animate-pulse rounded-lg bg-slate-100" /></div>)}</div></Card>;
}
