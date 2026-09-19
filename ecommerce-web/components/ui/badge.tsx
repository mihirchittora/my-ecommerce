import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva("inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.08em]", {
  variants: { variant: { default: "border-transparent bg-primary/10 text-primary", secondary: "border-transparent bg-secondary text-secondary-foreground", outline: "text-foreground", success: "border-transparent bg-emerald-50 text-emerald-700", warning: "border-transparent bg-amber-50 text-amber-700", danger: "border-transparent bg-rose-50 text-rose-700", muted: "border-transparent bg-slate-100 text-slate-600" } },
  defaultVariants: { variant: "default" },
});

export function Badge({ className, variant, ...props }: React.HTMLAttributes<HTMLDivElement> & VariantProps<typeof badgeVariants>) { return <div className={cn(badgeVariants({ variant }), className)} {...props} />; }
