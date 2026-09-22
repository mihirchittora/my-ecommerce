"use client";

import { AlertCircle, RefreshCcw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";

export function LoadingBlock({ label = "Loading" }: { label?: string }) {
  return <div className="flex min-h-40 items-center justify-center"><Spinner label={label} /></div>;
}

export function ErrorState({ message = "Something went wrong. Please try again.", retry }: { message?: string; retry?: () => void }) {
  return <div className="rounded-3xl border border-red-200 bg-red-50 p-8 text-center"><AlertCircle className="mx-auto mb-3 h-7 w-7 text-red-600" /><p className="text-sm text-red-900">{message}</p>{retry ? <Button variant="secondary" className="mt-5" onClick={retry}><RefreshCcw className="mr-2 h-4 w-4" />Try again</Button> : null}</div>;
}

export function EmptyState({ title, message, action }: { title: string; message?: string; action?: React.ReactNode }) {
  return <div className="rounded-3xl border border-dashed border-ink/20 bg-white p-10 text-center"><h2 className="font-display text-xl font-semibold text-ink">{title}</h2>{message ? <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-ink/60">{message}</p> : null}{action ? <div className="mt-5">{action}</div> : null}</div>;
}

export function ProductSkeletonGrid() {
  return <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">{Array.from({ length: 8 }, (_, index) => <div key={index} className="animate-pulse"><div className="aspect-[4/5] rounded-3xl bg-ink/10" /><div className="mt-4 h-4 w-4/5 rounded bg-ink/10" /><div className="mt-2 h-4 w-2/5 rounded bg-ink/10" /></div>)}</div>;
}
