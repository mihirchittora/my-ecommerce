"use client";

import Image from "next/image";
import { useState } from "react";
import type { Category } from "@/lib/types";
import { getAssetUrl } from "@/lib/utils";
import { cn } from "@/lib/utils";

export function CategoryImage({ category, className, sizes = "48px" }: { category: Pick<Category, "name" | "image">; className?: string; sizes?: string }) {
  const [failed, setFailed] = useState(false);
  const src = getAssetUrl(category.image?.url);
  if (!src || failed) {
    return <div role="img" aria-label={`${category.name} category image unavailable`} className={cn("grid h-full w-full place-items-center bg-slate-900 text-white", className)}><span className="grid h-7 w-7 place-items-center rounded-lg bg-white/15 text-xs font-black">M</span></div>;
  }
  return <Image src={src} alt={category.image?.altText || `${category.name} collection`} fill sizes={sizes} unoptimized onError={() => setFailed(true)} className={cn("object-cover", className)} />;
}
