"use client";

import Image from "next/image";
import { useState } from "react";
import { cn } from "@/lib/utils";

export function CatalogImage({
  src,
  alt,
  fallbackLabel = "Image coming soon",
  sizes,
  priority = false,
  className,
}: {
  src: string | null;
  alt: string;
  fallbackLabel?: string;
  sizes: string;
  priority?: boolean;
  className?: string;
}) {
  const [failed, setFailed] = useState(false);
  if (!src || failed) return <div role="img" aria-label={alt} className="grid h-full w-full place-items-center px-6 text-center text-sm text-ink/35">{fallbackLabel}</div>;
  return <Image src={src} alt={alt} fill priority={priority} sizes={sizes} unoptimized onError={() => setFailed(true)} className={cn("object-cover", className)} />;
}
