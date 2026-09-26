"use client";

import Image from "next/image";
import { CheckCircle2, ImagePlus, Loader2, Trash2, UploadCloud } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import type { Product } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { formatFileSize, getAssetUrl, getImageFileUrl } from "@/lib/utils";
import { useDeleteImage, useUploadImage } from "@/lib/queries";
import { ApiError } from "@/lib/api/client";
import { useToast } from "@/components/ui/toast";

const accepted = ["image/jpeg", "image/png", "image/webp"];
const maxBytes = 5 * 1024 * 1024;

export function ImageManager({ product }: { product: Product }) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [variantId, setVariantId] = useState("");
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadErrors, setUploadErrors] = useState<string[]>([]);
  const [previews, setPreviews] = useState<{ name: string; url: string }[]>([]);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const upload = useUploadImage(product.id);
  const remove = useDeleteImage(product.id);
  const { toast } = useToast();

  useEffect(() => () => previews.forEach((preview) => URL.revokeObjectURL(preview.url)), [previews]);

  const handleFiles = async (files: FileList | File[]) => {
    const list = Array.from(files);
    const errors: string[] = [];
    const valid = list.filter((file) => {
      if (!accepted.includes(file.type)) {
        errors.push(`${file.name}: use JPEG, PNG or WEBP.`);
        return false;
      }
      if (file.size > maxBytes) {
        errors.push(`${file.name}: maximum size is 5 MB.`);
        return false;
      }
      return true;
    });
    setUploadErrors(errors);
    if (valid.length === 0) return;
    setPreviews(valid.map((file) => ({ name: file.name, url: URL.createObjectURL(file) })));
    setUploading(true);
    try {
      for (const [index, file] of valid.entries()) {
        await upload.mutateAsync({ file, sortOrder: product.images.length + index, variantId: variantId || undefined });
      }
      toast({ title: valid.length === 1 ? "Image uploaded" : "Images uploaded", description: "Your product gallery is up to date." });
    } catch (error) {
      setUploadErrors((current) => [...current, error instanceof ApiError ? error.message : "Unable to upload the image."]);
    } finally {
      setUploading(false);
      setPreviews([]);
      if (inputRef.current) inputRef.current.value = "";
    }
  };

  const confirmDelete = () => {
    if (!deleteTarget) return;
    remove.mutate(deleteTarget, {
      onSuccess: () => {
        setDeleteTarget(null);
        toast({ title: "Image deleted" });
      },
      onError: (error) => setUploadErrors([error instanceof ApiError ? error.message : "Unable to delete the image."]),
    });
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between gap-4">
          <div>
            <CardTitle>Images</CardTitle>
            <p className="mt-1 text-sm text-muted-foreground">JPEG, PNG or WEBP up to 5 MB. Images are stored as files, never base64.</p>
          </div>
          <ImagePlus className="h-5 w-5 text-slate-300" />
        </div>
      </CardHeader>
      <CardContent>
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="flex-1">
            <label className="field-label" htmlFor="image-variant">
              Associate with variant <span className="font-normal text-slate-400">(optional)</span>
            </label>
            <Select id="image-variant" value={variantId} onChange={(event) => setVariantId(event.target.value)}>
              <option value="">Product image</option>
              {product.variants.map((variant) => <option key={variant.id} value={variant.id}>{variant.sku}</option>)}
            </Select>
          </div>
          <Button type="button" variant="outline" onClick={() => inputRef.current?.click()} disabled={uploading}>
            <UploadCloud className="h-4 w-4" />Choose files
          </Button>
          <input ref={inputRef} type="file" className="sr-only" accept={accepted.join(",")} multiple onChange={(event) => event.target.files && void handleFiles(event.target.files)} />
        </div>

        <div
          onDragEnter={(event) => { event.preventDefault(); setDragging(true); }}
          onDragOver={(event) => event.preventDefault()}
          onDragLeave={() => setDragging(false)}
          onDrop={(event) => { event.preventDefault(); setDragging(false); void handleFiles(event.dataTransfer.files); }}
          className={`rounded-2xl border border-dashed px-6 py-8 text-center transition-colors ${dragging ? "border-primary bg-blue-50" : "border-slate-200 bg-slate-50/60"}`}
        >
          <UploadCloud className="mx-auto h-7 w-7 text-slate-400" />
          <p className="mt-3 text-sm font-semibold text-slate-700">Drop images here to upload</p>
          <p className="mt-1 text-xs text-slate-500">or use the file picker above</p>
          {uploading && <p className="mt-3 inline-flex items-center gap-2 text-xs font-semibold text-primary"><Loader2 className="h-3.5 w-3.5 animate-spin" />Uploading…</p>}
        </div>

        {previews.length > 0 && <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {previews.map((preview) => <div className="overflow-hidden rounded-xl border border-blue-100 bg-blue-50" key={preview.url}>
            <Image src={preview.url} alt={`Preview of ${preview.name}`} width={320} height={320} sizes="(max-width: 640px) 50vw, 25vw" unoptimized className="aspect-square w-full object-cover" />
            <p className="truncate px-2 py-1.5 text-[11px] font-medium text-blue-800">{preview.name}</p>
          </div>)}
        </div>}

        {uploadErrors.length > 0 && <div role="alert" className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">
          <ul className="space-y-1">{uploadErrors.map((error, index) => <li key={`${error}-${index}`}>{error}</li>)}</ul>
        </div>}

        {product.images.length > 0 && <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {product.images.slice().sort((a, b) => a.sortOrder - b.sortOrder).map((image) => <div key={image.id} className="group relative aspect-square overflow-hidden rounded-2xl border border-slate-200 bg-slate-50">
            <Image src={getAssetUrl(image.url) ?? getImageFileUrl(product.id, image.id)} alt={image.originalFilename ?? `${product.name} image`} fill sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw" unoptimized className="object-cover" />
            <div className="absolute inset-x-0 bottom-0 flex items-center justify-between bg-slate-950/75 px-2.5 py-2 text-[11px] text-white opacity-0 transition-opacity group-hover:opacity-100 group-focus-within:opacity-100">
              <span className="truncate">{image.originalFilename ?? "Product image"} · {formatFileSize(image.sizeBytes)}</span>
              <Button type="button" variant="ghost" size="icon-sm" className="shrink-0 text-white hover:bg-white/20 hover:text-white" onClick={() => setDeleteTarget(image.id)} aria-label="Delete image"><Trash2 className="h-3.5 w-3.5" /></Button>
            </div>
            <div className="absolute left-2 top-2 rounded-full bg-white/90 px-2 py-1 text-[10px] font-semibold text-slate-600">#{image.sortOrder + 1}</div>
          </div>)}
        </div>}

        {product.images.length === 0 && <div className="mt-5 flex items-center justify-center gap-2 text-xs text-slate-400"><CheckCircle2 className="h-4 w-4" />No images uploaded yet</div>}
      </CardContent>
      <Dialog open={Boolean(deleteTarget)} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete this image?</DialogTitle>
            <DialogDescription>This will remove the stored file from the catalog. This action cannot be undone.</DialogDescription>
          </DialogHeader>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>Cancel</Button>
            <Button variant="destructive" onClick={confirmDelete} disabled={remove.isPending}>{remove.isPending ? "Deleting…" : "Delete image"}</Button>
          </div>
        </DialogContent>
      </Dialog>
    </Card>
  );
}
