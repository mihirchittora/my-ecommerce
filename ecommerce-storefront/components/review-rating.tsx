import type { ReviewSummary } from "@/lib/api/catalog";
import { cn } from "@/lib/utils";

type ReviewRatingProps = {
  summary?: ReviewSummary;
  isLoading?: boolean;
  className?: string;
};

const stars = Array.from({ length: 5 }, (_, index) => index);

export function ReviewRating({ summary, isLoading = false, className }: ReviewRatingProps) {
  const average = Math.max(0, Math.min(5, summary?.averageRating ?? 0));
  const reviewCount = summary?.reviewCount ?? 0;
  const fillWidth = `${(average / 5) * 100}%`;
  const label = isLoading ? "Loading ratings" : reviewCount ? `${average.toFixed(1)} (${reviewCount})` : "No reviews yet";

  return <div className={cn("flex min-w-0 items-center gap-2", className)} aria-label={isLoading ? "Loading ratings" : reviewCount ? `${average.toFixed(1)} out of 5 from ${reviewCount} review${reviewCount === 1 ? "" : "s"}` : "No reviews yet"}>
    <span className="relative inline-flex shrink-0 leading-none" aria-hidden="true">
      <span className="flex text-ink/15">{stars.map((star) => <span key={star} className="text-[1.05rem]">★</span>)}</span>
      <span className="absolute inset-y-0 left-0 flex overflow-hidden text-coral" style={{ width: fillWidth }}>{stars.map((star) => <span key={star} className="text-[1.05rem]">★</span>)}</span>
    </span>
    <span className="truncate text-sm font-semibold text-ink/65">{label}</span>
  </div>;
}
