import { cn } from "@/lib/utils";

export function Badge({ children, tone = "neutral" }: { children: React.ReactNode; tone?: "neutral" | "success" | "warning" | "danger" }) {
  return <span className={cn("inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold", {
    "bg-mist text-moss": tone === "neutral",
    "bg-emerald-100 text-emerald-800": tone === "success",
    "bg-amber-100 text-amber-800": tone === "warning",
    "bg-red-100 text-red-800": tone === "danger",
  })}>{children}</span>;
}
