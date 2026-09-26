"use client";

import Image from "next/image";
import { ImagePlus, Loader2, Trash2, UploadCloud } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/components/auth-provider";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";
import { siteSettingsApi } from "@/lib/api/site-settings";
import { hasPermission } from "@/lib/permissions";
import { formatFileSize, getAssetUrl } from "@/lib/utils";
import type { CarouselSlide } from "@/lib/types";

const acceptedTypes = ["image/jpeg", "image/png", "image/webp"];
const maxBytes = 5 * 1024 * 1024;

type PendingImage = { file?: File; previewUrl?: string; remove: boolean };

function validateImage(file: File | undefined) {
  if (!file) return "Choose an image first.";
  if (!acceptedTypes.includes(file.type)) return "Please upload a JPG, PNG, or WebP image.";
  if (file.size > maxBytes) return "Image must be smaller than 5 MB.";
  return undefined;
}

function SlideImageCard({ slide, pending, canManage, onChoose, onRemove, onSave, isSaving }: { slide: CarouselSlide; pending: PendingImage; canManage: boolean; onChoose: (file: File | undefined) => void; onRemove: () => void; onSave: () => void; isSaving: boolean }) {
  const inputRef = useRef<HTMLInputElement>(null);
  const imageUrl = pending.remove ? null : pending.previewUrl ?? getAssetUrl(slide.imageUrl);
  return <Card className="overflow-hidden"><div className="relative aspect-[16/8] bg-slate-100">{imageUrl ? <Image src={imageUrl} alt={slide.headline} fill sizes="(min-width: 1024px) 460px, 90vw" unoptimized className="object-cover" /> : <div className="flex h-full items-center justify-center text-sm font-semibold text-slate-400"><ImagePlus className="mr-2 h-5 w-5" /> No slide image</div>}</div><CardHeader className="pb-3"><div className="flex items-start justify-between gap-4"><div><p className="text-[10px] font-bold uppercase tracking-[0.16em] text-primary">Slide {slide.sortOrder + 1}</p><CardTitle className="mt-1">{slide.headline}</CardTitle><CardDescription className="mt-1">{slide.eyebrow ?? "No eyebrow text"}</CardDescription></div><span className="rounded-full bg-slate-100 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-[0.12em] text-slate-500">{slide.active ? "Live" : "Hidden"}</span></div></CardHeader><CardContent><div className="flex flex-wrap gap-2"><Button type="button" variant="outline" disabled={!canManage} onClick={() => inputRef.current?.click()}><UploadCloud className="h-4 w-4" />{imageUrl ? "Replace image" : "Upload image"}</Button>{imageUrl ? <Button type="button" variant="ghost" disabled={!canManage} className="text-rose-600 hover:text-rose-700" onClick={onRemove}><Trash2 className="h-4 w-4" />Remove</Button> : null}<input ref={inputRef} type="file" className="sr-only" accept={acceptedTypes.join(",")} onChange={(event) => onChoose(event.target.files?.[0])} /></div>{pending.file ? <p className="mt-3 text-xs text-slate-500">Selected: <span className="font-semibold text-slate-700">{pending.file.name} · {formatFileSize(pending.file.size)}</span></p> : null}{pending.remove ? <p className="mt-3 text-xs text-rose-600">This image will be removed when you save.</p> : null}<Button type="button" className="mt-4" disabled={!canManage || isSaving || (!pending.file && !pending.remove)} onClick={onSave}>{isSaving ? <><Loader2 className="h-4 w-4 animate-spin" />Saving…</> : "Save slide image"}</Button></CardContent></Card>;
}

export default function StorefrontSettingsPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const settings = useQuery({ queryKey: ["admin", "site-settings"], queryFn: siteSettingsApi.get });
  const [siteTitle, setSiteTitle] = useState("");
  const [logo, setLogo] = useState<PendingImage>({ remove: false });
  const [slideImages, setSlideImages] = useState<Record<string, PendingImage>>({});
  const [imageError, setImageError] = useState<string>();
  const canManage = hasPermission(user, "SITE_SETTINGS_UPDATE");

  useEffect(() => {
    if (!settings.data) return;
    setSiteTitle(settings.data.siteTitle);
    setLogo({ remove: false });
    setSlideImages(Object.fromEntries(settings.data.slides.map((slide) => [slide.id, { remove: false }])));
  }, [settings.data]);

  const refresh = () => void queryClient.invalidateQueries({ queryKey: ["admin", "site-settings"] });
  const branding = useMutation({
    mutationFn: async () => {
      let next = await siteSettingsApi.update({ siteTitle: siteTitle.trim() });
      if (logo.file) next = await siteSettingsApi.uploadLogo(logo.file);
      else if (logo.remove && next.logoUrl) next = await siteSettingsApi.deleteLogo();
      return next;
    },
    onSuccess: refresh,
  });
  const slideImage = useMutation({
    mutationFn: async ({ slideId, pending }: { slideId: string; pending: PendingImage }) => pending.file ? siteSettingsApi.uploadSlideImage(slideId, pending.file) : siteSettingsApi.deleteSlideImage(slideId),
    onSuccess: refresh,
  });

  const chooseLogo = (file: File | undefined) => {
    const error = validateImage(file);
    if (error) { setImageError(error); return; }
    setImageError(undefined);
    setLogo({ file, previewUrl: file ? URL.createObjectURL(file) : undefined, remove: false });
  };
  const chooseSlide = (slideId: string, file: File | undefined) => {
    const error = validateImage(file);
    if (error) { setImageError(error); return; }
    setImageError(undefined);
    setSlideImages((current) => ({ ...current, [slideId]: { file, previewUrl: file ? URL.createObjectURL(file) : undefined, remove: false } }));
  };
  const currentLogoUrl = logo.remove ? null : logo.previewUrl ?? getAssetUrl(settings.data?.logoUrl);
  const apiError = branding.error ?? slideImage.error;
  const errorMessage = apiError instanceof ApiError ? apiError.message : apiError ? "Could not save storefront settings. Please try again." : undefined;

  if (settings.isLoading) return <LoadingCard rows={7} />;
  if (settings.isError || !settings.data) return <ErrorState title="Storefront settings unavailable" message="Catalog Service could not be reached." onRetry={() => void settings.refetch()} />;

  return <>
    <PageIntro eyebrow="Storefront" title="Storefront settings" description="Update the website identity and the images shown in the homepage carousel. Changes are published as soon as you save them." />
    <div className="grid gap-6 xl:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
      <Card><CardHeader><CardTitle>Website identity</CardTitle><CardDescription>Keep the name and logo used in the storefront header and footer in sync.</CardDescription></CardHeader><CardContent className="space-y-6">
        <div><Label htmlFor="site-title">Website title</Label><Input id="site-title" className="mt-2" value={siteTitle} disabled={!canManage} maxLength={160} onChange={(event) => setSiteTitle(event.target.value)} /><p className="mt-1.5 text-xs text-slate-500">This is shown beside the logo and in the footer.</p></div>
        <div><div className="flex items-start justify-between gap-4"><div><Label>Website logo</Label><p className="mt-1 text-xs text-slate-500">JPG, PNG or WebP up to 5 MB.</p></div><ImagePlus className="h-5 w-5 text-slate-300" /></div><div className="mt-4 flex items-center gap-4"><div className="relative grid h-20 w-20 shrink-0 place-items-center overflow-hidden rounded-2xl border border-slate-200 bg-slate-950 text-2xl font-black text-white">{currentLogoUrl ? <Image src={currentLogoUrl} alt="Current website logo" fill sizes="80px" unoptimized className="object-contain" /> : siteTitle.slice(0, 1).toUpperCase()}</div><div className="flex flex-wrap gap-2"><label className={`inline-flex h-10 cursor-pointer items-center gap-2 rounded-xl border border-input bg-background px-4 text-sm font-medium transition hover:bg-accent ${!canManage ? "pointer-events-none opacity-60" : ""}`}><UploadCloud className="h-4 w-4" />{currentLogoUrl ? "Replace logo" : "Upload logo"}<input type="file" className="sr-only" accept={acceptedTypes.join(",")} disabled={!canManage} onChange={(event) => chooseLogo(event.target.files?.[0])} /></label>{currentLogoUrl ? <Button type="button" variant="ghost" disabled={!canManage} className="text-rose-600 hover:text-rose-700" onClick={() => setLogo({ remove: true })}><Trash2 className="h-4 w-4" />Remove</Button> : null}</div></div>{logo.file ? <p className="mt-3 text-xs text-slate-500">Selected: <span className="font-semibold text-slate-700">{logo.file.name} · {formatFileSize(logo.file.size)}</span></p> : null}</div>
        {imageError ? <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2.5 text-xs text-rose-700">{imageError}</p> : null}{errorMessage ? <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2.5 text-sm text-rose-700">{errorMessage}</p> : null}
        {canManage ? <Button disabled={branding.isPending || !siteTitle.trim()} onClick={() => branding.mutate()}>{branding.isPending ? <><Loader2 className="h-4 w-4 animate-spin" />Saving…</> : "Save identity"}</Button> : <p className="text-sm text-slate-500">You have read-only access. Branding changes require SITE_SETTINGS_UPDATE.</p>}
      </CardContent></Card>
      <div><div className="mb-4"><h2 className="text-base font-semibold tracking-tight text-slate-950">Homepage carousel</h2><p className="mt-1 text-sm text-slate-500">Upload one image per slide. Without an image, the storefront uses the soft card illustration from the reference design.</p></div><div className="grid gap-4">{settings.data.slides.map((slide) => <SlideImageCard key={slide.id} slide={slide} pending={slideImages[slide.id] ?? { remove: false }} canManage={canManage} onChoose={(file) => chooseSlide(slide.id, file)} onRemove={() => setSlideImages((current) => ({ ...current, [slide.id]: { remove: true } }))} onSave={() => { const pending = slideImages[slide.id]; if (pending) slideImage.mutate({ slideId: slide.id, pending }); }} isSaving={slideImage.isPending && slideImage.variables?.slideId === slide.id} />)}</div></div>
    </div>
  </>;
}
