"use client";

import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { catalogApi } from "@/lib/api/catalog";
import type { Category } from "@/lib/types";
import { cn, getAssetUrl, siteTitleOrDefault } from "@/lib/utils";

export function CategoryImage({ category, className, sizes = "(max-width: 640px) 50vw, 25vw", priority = false }: { category: Pick<Category, "name" | "image">; className?: string; sizes?: string; priority?: boolean }) {
  const [failed, setFailed] = useState(false);
  const siteSettings = useQuery({ queryKey: ["site-settings"], queryFn: catalogApi.getSiteSettings, staleTime: 60_000 });
  const siteTitle = siteTitleOrDefault(siteSettings.data?.siteTitle);
  const src = getAssetUrl(category.image?.url);
  if (!src || failed) {
    return <div role="img" aria-label={`${category.name} category image unavailable`} className={cn("grid h-full w-full place-items-center bg-[#19352f] text-white", className)}><div className="text-center"><span className="mx-auto grid h-11 w-11 place-items-center rounded-2xl bg-white/12 font-display text-lg font-black">{siteTitle.slice(0, 1).toUpperCase()}</span><span className="mt-2 block text-[10px] font-bold uppercase tracking-[0.16em] text-white/60">{siteTitle}</span></div></div>;
  }
  return <Image src={src} alt={category.image?.altText || `${category.name} collection`} fill sizes={sizes} priority={priority} unoptimized onError={() => setFailed(true)} className={cn("object-cover", className)} />;
}
