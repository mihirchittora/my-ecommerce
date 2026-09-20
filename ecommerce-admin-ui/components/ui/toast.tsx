"use client";

import { CheckCircle2, X } from "lucide-react";
import * as React from "react";
import { cn } from "@/lib/utils";

type ToastItem = { id: number; title: string; description?: string; variant?: "default" | "destructive" };
type ToastContextValue = { toast: (item: Omit<ToastItem, "id">) => void };
const ToastContext = React.createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = React.useState<ToastItem[]>([]);
  const toast = React.useCallback((item: Omit<ToastItem, "id">) => {
    const id = Date.now() + Math.floor(Math.random() * 1000);
    setItems((current) => [...current, { ...item, id }]);
    window.setTimeout(() => setItems((current) => current.filter((toastItem) => toastItem.id !== id)), 4500);
  }, []);
  return <ToastContext.Provider value={{ toast }}>{children}<div className="fixed bottom-4 right-4 z-[100] flex w-[calc(100%-2rem)] max-w-sm flex-col gap-3" aria-live="polite">{items.map((item) => <div key={item.id} className={cn("relative rounded-2xl border bg-card p-4 pr-10 shadow-soft", item.variant === "destructive" && "border-rose-200")}><button className="absolute right-3 top-3 text-muted-foreground hover:text-foreground" onClick={() => setItems((current) => current.filter((toastItem) => toastItem.id !== item.id))} aria-label="Dismiss notification"><X className="h-4 w-4" /></button><div className="flex gap-3"><CheckCircle2 className={cn("mt-0.5 h-4 w-4 shrink-0 text-emerald-600", item.variant === "destructive" && "text-rose-600")} /><div><p className="text-sm font-semibold">{item.title}</p>{item.description && <p className="mt-1 text-sm text-muted-foreground">{item.description}</p>}</div></div></div>)}</div></ToastContext.Provider>;
}

export function useToast() {
  const context = React.useContext(ToastContext);
  if (!context) throw new Error("useToast must be used within ToastProvider");
  return context;
}
