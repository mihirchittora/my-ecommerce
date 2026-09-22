import { forwardRef } from "react";
import Link from "next/link";
import { cn } from "@/lib/utils";

type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: "primary" | "secondary" | "ghost" | "danger"; asLink?: string };

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button({ className, variant = "primary", asLink, ...props }, ref) {
  const styles = cn("inline-flex min-h-11 items-center justify-center rounded-full px-5 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-coral focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50", {
    "bg-ink text-white hover:bg-moss": variant === "primary",
    "border border-ink/15 bg-white text-ink hover:border-moss hover:text-moss": variant === "secondary",
    "text-ink hover:bg-mist": variant === "ghost",
    "bg-red-600 text-white hover:bg-red-700": variant === "danger",
  }, className);
  if (asLink) return <Link href={asLink} className={styles}>{props.children}</Link>;
  return <button ref={ref} className={styles} {...props} />;
});
