import { cn } from "@/lib/utils";

export function Card({ children, className }: { children: React.ReactNode; className?: string }) {
  return <section className={cn("rounded-3xl border border-ink/10 bg-white shadow-soft", className)}>{children}</section>;
}
