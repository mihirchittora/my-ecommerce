import { forwardRef } from "react";
import { cn } from "@/lib/utils";

export const Input = forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(function Input({ className, ...props }, ref) {
  return <input ref={ref} className={cn("h-12 w-full rounded-2xl border border-ink/15 bg-white px-4 text-sm text-ink outline-none transition placeholder:text-ink/40 focus:border-moss focus:ring-2 focus:ring-moss/15", className)} {...props} />;
});
