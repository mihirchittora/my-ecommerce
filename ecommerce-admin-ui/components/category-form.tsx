"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Image from "next/image";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { ImagePlus, Loader2, Trash2, UploadCloud } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import type { CategoryNode, CategoryStatus } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { ApiError } from "@/lib/api/client";
import { getAssetUrl, formatFileSize } from "@/lib/utils";

const accepted = ["image/jpeg", "image/png", "image/webp"];
const maxBytes = 5 * 1024 * 1024;
const schema = z.object({
  name: z.string().trim().min(1, "Category name is required.").max(120, "Use 120 characters or fewer."),
  slug: z.string().trim().max(150, "Use 150 characters or fewer.").optional(),
  description: z.string().trim().max(500, "Use 500 characters or fewer.").optional(),
  parentId: z.string().optional(),
  status: z.enum(["ACTIVE", "INACTIVE"]),
  altText: z.string().trim().max(255, "Use 255 characters or fewer.").optional(),
});
type Values = z.infer<typeof schema>;
export type CategoryFormValues = Values & { imageFile?: File; removeImage: boolean };

function flatten(nodes: CategoryNode[], depth = 0): Array<{ id: string; label: string; depth: number }> { return nodes.flatMap((node) => [{ id: node.id, label: node.name, depth }, ...flatten(node.children, depth + 1)]); }

export function CategoryForm({ categories, initial, parentId, onSubmit, onCancel, isSubmitting, serverError, canManageImage = true }: { categories: CategoryNode[]; initial?: { name: string; slug: string; description: string | null; parentId: string | null; status: CategoryStatus; image: { url: string; altText: string } | null }; parentId?: string; onSubmit: (values: CategoryFormValues) => void; onCancel: () => void; isSubmitting?: boolean; serverError?: unknown; canManageImage?: boolean }) {
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { name: initial?.name ?? "", slug: initial?.slug ?? "", description: initial?.description ?? "", parentId: initial?.parentId ?? parentId ?? "", status: initial?.status ?? "ACTIVE", altText: initial?.image?.altText ?? "" } });
  const inputRef = useRef<HTMLInputElement>(null);
  const [imageFile, setImageFile] = useState<File>();
  const [previewUrl, setPreviewUrl] = useState<string>();
  const [removeImage, setRemoveImage] = useState(false);
  const [imageError, setImageError] = useState<string>();
  useEffect(() => () => { if (previewUrl) URL.revokeObjectURL(previewUrl); }, [previewUrl]);
  const errorMessage = serverError instanceof ApiError ? serverError.message : serverError ? "Please review the category and try again." : undefined;
  const options = flatten(categories).filter((category) => category.id !== initial?.parentId);
  const chooseFile = (file?: File) => {
    if (!file) return;
    if (!accepted.includes(file.type)) { setImageError("Please upload a JPG, PNG, or WebP image."); return; }
    if (file.size > maxBytes) { setImageError("Image must be smaller than 5 MB."); return; }
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setImageError(undefined);
    setImageFile(file);
    setPreviewUrl(URL.createObjectURL(file));
    setRemoveImage(false);
  };
  const currentImageUrl = initial?.image && !removeImage ? getAssetUrl(initial.image.url) : null;
  const submit = (values: Values) => onSubmit({ ...values, imageFile, removeImage });
  return <form className="space-y-5" onSubmit={form.handleSubmit(submit)} noValidate>
    <div><Label htmlFor="category-name">Name</Label><Input id="category-name" className="mt-2" placeholder="e.g. Audio & headphones" {...form.register("name")} />{form.formState.errors.name && <p className="mt-1.5 text-xs text-rose-600">{form.formState.errors.name.message}</p>}</div>
    <div><Label htmlFor="category-slug">Slug <span className="font-normal text-muted-foreground">(optional)</span></Label><Input id="category-slug" className="mt-2" placeholder="audio-headphones" {...form.register("slug")} />{form.formState.errors.slug && <p className="mt-1.5 text-xs text-rose-600">{form.formState.errors.slug.message}</p>}</div>
    <div><Label htmlFor="category-description">Description <span className="font-normal text-muted-foreground">(optional)</span></Label><textarea id="category-description" className="mt-2 flex min-h-20 w-full rounded-xl border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/20" placeholder="A short customer-friendly description" {...form.register("description")} />{form.formState.errors.description && <p className="mt-1.5 text-xs text-rose-600">{form.formState.errors.description.message}</p>}</div>
    <div><Label htmlFor="category-parent">Parent category</Label><Select id="category-parent" className="mt-2" {...form.register("parentId")}><option value="">Root category</option>{options.map((option) => <option key={option.id} value={option.id}>{"— ".repeat(option.depth)}{option.label}</option>)}</Select></div>
    {initial && <div><Label htmlFor="category-status">Status</Label><Select id="category-status" className="mt-2" {...form.register("status")}><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></Select></div>}
    <section className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4" aria-labelledby="category-image-heading"><div className="flex items-start justify-between gap-4"><div><h3 id="category-image-heading" className="text-sm font-semibold text-slate-800">Category image</h3><p className="mt-1 text-xs leading-5 text-slate-500">JPG, PNG or WebP up to 5 MB. The image is uploaded when you save.</p></div><ImagePlus className="h-5 w-5 text-slate-300" /></div><div className="mt-4 flex flex-col gap-4 sm:flex-row"><div className="relative h-32 w-full shrink-0 overflow-hidden rounded-xl border border-slate-200 bg-white sm:w-40">{previewUrl ? <Image src={previewUrl} alt="New category image preview" fill sizes="160px" unoptimized className="object-cover" /> : currentImageUrl ? <Image src={currentImageUrl} alt={initial?.image?.altText ?? "Current category image"} fill sizes="160px" unoptimized className="object-cover" /> : <div className="grid h-full place-items-center px-4 text-center text-xs font-semibold text-slate-400"><span><span className="mx-auto mb-2 grid h-9 w-9 place-items-center rounded-xl bg-slate-900 text-sm font-black text-white">M</span>No image yet</span></div>}</div><div className="min-w-0 flex-1">{canManageImage ? <><div className="flex flex-wrap gap-2"><Button type="button" variant="outline" onClick={() => inputRef.current?.click()}><UploadCloud className="h-4 w-4" />{currentImageUrl || previewUrl ? "Replace image" : "Upload image"}</Button>{(currentImageUrl || previewUrl) && <Button type="button" variant="ghost" className="text-rose-600 hover:text-rose-700" onClick={() => { setRemoveImage(true); setImageFile(undefined); if (previewUrl) URL.revokeObjectURL(previewUrl); setPreviewUrl(undefined); }}><Trash2 className="h-4 w-4" />Remove</Button>}</div><input ref={inputRef} type="file" className="sr-only" accept={accepted.join(",")} onChange={(event) => chooseFile(event.target.files?.[0])} /><div onDragOver={(event) => event.preventDefault()} onDrop={(event) => { event.preventDefault(); chooseFile(event.dataTransfer.files?.[0]); }} className="mt-3 rounded-xl border border-dashed border-slate-200 bg-white px-3 py-3 text-xs text-slate-500">Drop an image here or choose a file. {imageFile ? <span className="font-semibold text-slate-700">{imageFile.name} · {formatFileSize(imageFile.size)}</span> : null}</div></> : <p className="text-xs leading-5 text-slate-500">Category image changes require the `CATEGORY_UPDATE` permission.</p>}</div></div><div className="mt-4"><Label htmlFor="category-alt-text">Alt text</Label><Input id="category-alt-text" disabled={!canManageImage} className="mt-2 bg-white" placeholder="Women's fashion collection" {...form.register("altText")} />{form.formState.errors.altText && <p className="mt-1.5 text-xs text-rose-600">{form.formState.errors.altText.message}</p>}</div>{imageError && <p role="alert" className="mt-3 text-xs text-rose-600">{imageError}</p>}</section>
    {errorMessage && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2.5 text-sm text-rose-700">{errorMessage}{serverError instanceof ApiError && serverError.details.length > 0 && <ul className="mt-1 list-disc pl-4 text-xs">{serverError.details.map((detail) => <li key={detail}>{detail}</li>)}</ul>}</div>}
    <div className="flex justify-end gap-2"><Button type="button" variant="outline" onClick={onCancel}>Cancel</Button><Button type="submit" disabled={isSubmitting}>{isSubmitting ? <><Loader2 className="h-4 w-4 animate-spin" />Saving…</> : initial ? "Save changes" : "Create category"}</Button></div>
  </form>;
}
