import { cn } from "@/lib/utils";

export function Card({ children, className, id }: { children: React.ReactNode; className?: string; id?: string }) {
  return <section id={id} className={cn("rounded-3xl border border-ink/10 bg-white shadow-soft", className)}>{children}</section>;
}
