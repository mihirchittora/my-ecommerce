"use client";

import Image from "next/image";
import Link from "next/link";
import { ArrowRight, BadgeCheck, ChevronLeft, ChevronRight, Coffee, ShoppingBag } from "lucide-react";
import { useEffect, useState } from "react";
import type { CarouselSlide } from "@/lib/types";
import { cn, getAssetUrl } from "@/lib/utils";

const fallbackSlides: CarouselSlide[] = [
  {
    id: "fallback-1", sortOrder: 0, eyebrow: "Thoughtful shopping, made simple", headline: "Everyday goods, at prices that feel good.", description: "Browse useful, beautiful pieces across the categories you reach for most.", primaryCtaLabel: "Shop all products", primaryCtaUrl: "/products", secondaryCtaLabel: "Explore categories", secondaryCtaUrl: "/categories", imageUrl: null, active: true, originalFilename: null, contentType: null, sizeBytes: null, updatedAt: "",
  },
];

export function HomeHeroCarousel({ slides }: { slides?: CarouselSlide[] }) {
  const visibleSlides = slides?.length ? slides : fallbackSlides;
  const [activeIndex, setActiveIndex] = useState(0);
  const [paused, setPaused] = useState(false);

  useEffect(() => {
    if (activeIndex >= visibleSlides.length) setActiveIndex(0);
  }, [activeIndex, visibleSlides.length]);

  useEffect(() => {
    if (paused || visibleSlides.length < 2) return;
    const timer = window.setInterval(() => setActiveIndex((index) => (index + 1) % visibleSlides.length), 6500);
    return () => window.clearInterval(timer);
  }, [paused, visibleSlides.length]);

  const selectSlide = (index: number) => setActiveIndex((index + visibleSlides.length) % visibleSlides.length);

  return <section className="page-shell pt-5 md:pt-8">
    <div
      className="relative overflow-hidden rounded-[2rem] bg-[#f2d5ce] px-6 py-8 shadow-soft sm:px-10 md:px-14 md:py-12"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={() => setPaused(false)}
      role="region"
      aria-roledescription="carousel"
      aria-label="Featured storefront promotions"
    >
      <div className="absolute -right-16 -top-28 h-72 w-72 rounded-full bg-white/40 blur-3xl" />
      <div className="absolute -bottom-32 right-1/3 h-64 w-64 rounded-full bg-coral/20 blur-3xl" />
      <div className="relative min-h-[430px] md:min-h-[445px]">
        {visibleSlides.map((slide, index) => {
          const isActive = index === activeIndex;
          const slideImageUrl = getAssetUrl(slide.imageUrl);
          return <article key={slide.id} className={cn("absolute inset-0 grid items-center gap-8 transition-opacity duration-500 lg:grid-cols-[1.1fr_0.9fr]", isActive ? "opacity-100" : "pointer-events-none opacity-0")} aria-hidden={!isActive}>
            <div className="max-w-2xl">
              {slide.eyebrow ? <div className="inline-flex items-center gap-2 rounded-full bg-white/70 px-3 py-1.5 text-xs font-bold text-moss"><BadgeCheck className="h-4 w-4" /> {slide.eyebrow}</div> : null}
              <h1 className="mt-5 max-w-xl font-display text-4xl font-bold leading-[1.05] tracking-tight text-ink md:text-6xl">{slide.headline}</h1>
              {slide.description ? <p className="mt-5 max-w-lg text-base leading-7 text-ink/65 md:text-lg">{slide.description}</p> : null}
              <div className="mt-7 flex flex-wrap gap-3">
                {slide.primaryCtaLabel && slide.primaryCtaUrl ? <Link href={slide.primaryCtaUrl} tabIndex={isActive ? 0 : -1} className="inline-flex min-h-11 items-center rounded-full bg-ink px-6 text-sm font-bold text-white transition hover:bg-moss">{slide.primaryCtaLabel}<ArrowRight className="ml-2 h-4 w-4" /></Link> : null}
                {slide.secondaryCtaLabel && slide.secondaryCtaUrl ? <Link href={slide.secondaryCtaUrl} tabIndex={isActive ? 0 : -1} className="inline-flex min-h-11 items-center rounded-full border border-ink/15 bg-white/70 px-6 text-sm font-bold text-ink transition hover:bg-white">{slide.secondaryCtaLabel}</Link> : null}
              </div>
            </div>
            <div className="relative hidden min-h-[270px] justify-self-end sm:block lg:w-full">
              {slideImageUrl ? <div className="relative ml-auto h-[270px] w-full max-w-[390px] overflow-hidden rounded-[2rem] border border-white/60 bg-white/50 shadow-soft"><Image src={slideImageUrl} alt={slide.headline} fill sizes="(min-width: 1024px) 390px, 42vw" unoptimized className="object-cover" /></div> : <div className="grid w-full max-w-[340px] grid-cols-2 gap-3 lg:ml-auto"><div className="flex aspect-square flex-col justify-between rounded-3xl bg-white/75 p-5 shadow-sm"><Coffee className="h-7 w-7 text-moss" /><span className="font-display text-lg font-bold">Small rituals</span></div><div className="mt-8 flex aspect-square flex-col justify-between rounded-3xl bg-ink p-5 text-white shadow-sm"><ShoppingBag className="h-7 w-7 text-coral" /><span className="font-display text-lg font-bold">Better finds</span></div></div>}
            </div>
          </article>;
        })}
      </div>
      {visibleSlides.length > 1 ? <div className="relative mt-5 flex items-center justify-between gap-4 border-t border-ink/10 pt-4"><div className="flex items-center gap-2" role="tablist" aria-label="Choose featured slide">{visibleSlides.map((slide, index) => <button key={slide.id} type="button" role="tab" aria-selected={index === activeIndex} aria-label={`Show slide ${index + 1}`} onClick={() => selectSlide(index)} className={cn("h-2 rounded-full transition-all", index === activeIndex ? "w-8 bg-ink" : "w-2 bg-ink/25 hover:bg-ink/50")} />)}</div><div className="flex items-center gap-2"><span className="text-xs font-bold tabular-nums text-ink/50">{String(activeIndex + 1).padStart(2, "0")} / {String(visibleSlides.length).padStart(2, "0")}</span><button type="button" aria-label="Previous slide" onClick={() => selectSlide(activeIndex - 1)} className="grid h-9 w-9 place-items-center rounded-full border border-ink/15 bg-white/60 text-ink transition hover:bg-white"><ChevronLeft className="h-4 w-4" /></button><button type="button" aria-label="Next slide" onClick={() => selectSlide(activeIndex + 1)} className="grid h-9 w-9 place-items-center rounded-full border border-ink/15 bg-white/60 text-ink transition hover:bg-white"><ChevronRight className="h-4 w-4" /></button></div></div> : null}
    </div>
  </section>;
}
