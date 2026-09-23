"use client";

import Image from "next/image";
import { useState } from "react";
import type { Category } from "@/lib/types";
import { cn, getAssetUrl } from "@/lib/utils";

export function CategoryImage({ category, className, sizes = "(max-width: 640px) 50vw, 25vw", priority = false }: { category: Pick<Category, "name" | "image">; className?: string; sizes?: string; priority?: boolean }) {
  const [failed, setFailed] = useState(false);
  const src = getAssetUrl(category.image?.url);
  if (!src || failed) {
    return <div role="img" aria-label={`${category.name} category image unavailable`} className={cn("grid h-full w-full place-items-center bg-[#19352f] text-white", className)}><div className="text-center"><span className="mx-auto grid h-11 w-11 place-items-center rounded-2xl bg-white/12 font-display text-lg font-black">M</span><span className="mt-2 block text-[10px] font-bold uppercase tracking-[0.16em] text-white/60">Morrow</span></div></div>;
  }
  return <Image src={src} alt={category.image?.altText || `${category.name} collection`} fill sizes={sizes} priority={priority} unoptimized onError={() => setFailed(true)} className={cn("object-cover", className)} />;
}
