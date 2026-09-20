"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Edit3, Layers3, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { PageIntro } from "@/components/page-intro";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { ImageManager } from "@/components/products/image-manager";
import { ProductInventoryPanel } from "@/components/inventory/product-inventory-panel";
import { ProductForm } from "@/components/products/product-form";
import { StatusBadge } from "@/components/products/status-badge";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useCategoryTree, useDeleteProduct, useProduct, useUpdateProduct } from "@/lib/queries";
import { useToast } from "@/components/ui/toast";
import type { CategoryNode } from "@/lib/types";
import { ApiError } from "@/lib/api/client";
import { formatCurrency, formatDateOnly, isExpired } from "@/lib/utils";

function findCategory(nodes: CategoryNode[], id: string): string { for (const node of nodes) { if (node.id === id) return node.name; const nested = findCategory(node.children, id); if (nested) return nested; } return "Unassigned category"; }

export default function ProductDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const router = useRouter();
  const product = useProduct(id);
  const categories = useCategoryTree();
  const update = useUpdateProduct(id);
  const remove = useDeleteProduct();
  const { toast } = useToast();
  const [editing, setEditing] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  useEffect(() => { setEditing(new URLSearchParams(window.location.search).get("edit") === "1"); }, []);
  if (product.isLoading || categories.isLoading) return <LoadingCard rows={7} />;
  if (product.isError || !product.data) return <ErrorState title="Product not found" message={product.error instanceof ApiError ? product.error.message : "We could not find this product."} onRetry={() => void product.refetch()} />;
  const item = product.data;
  const categoryName = findCategory(categories.data ?? [], item.categoryId);
  const submit = (payload: Parameters<typeof update.mutate>[0]) => update.mutate(payload, { onSuccess: () => { setEditing(false); toast({ title: "Product updated", description: "Your product details are saved." }); } });
  const deleteProduct = () => remove.mutate(item.id, { onSuccess: () => { toast({ title: "Product deleted" }); router.push("/products"); } });
  return <>{editing ? <><div className="mb-5"><Button variant="ghost" size="sm" onClick={() => setEditing(false)}><ArrowLeft className="h-4 w-4" />Back to product</Button></div><PageIntro eyebrow="Assortment" title={`Edit ${item.name}`} description="Update product details and variants. Image changes are managed in the gallery below." /><ProductForm categories={categories.data ?? []} product={item} onSubmit={submit} onCancel={() => setEditing(false)} isSubmitting={update.isPending} serverError={update.error} submitLabel="Save changes" /></> : <><div className="mb-5 flex items-center justify-between"><Button asChild variant="ghost" size="sm"><Link href="/products"><ArrowLeft className="h-4 w-4" />Back to products</Link></Button><div className="flex gap-2"><Button variant="outline" onClick={() => setEditing(true)}><Edit3 className="h-4 w-4" />Edit product</Button><Button variant="outline" className="text-rose-600 hover:text-rose-700" onClick={() => setDeleteOpen(true)}><Trash2 className="h-4 w-4" />Delete</Button></div></div><PageIntro eyebrow="Product details" title={item.name} description={item.description || "No product description has been added yet."} /><div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]"><Card><CardHeader className="flex-row items-start justify-between"><div><CardTitle>Product information</CardTitle><p className="mt-1 text-sm text-muted-foreground">/{item.slug}</p></div><div className="flex items-center gap-2"><StatusBadge status={item.status} />{isExpired(item.expiryDate) && <Badge variant="danger">Expired</Badge>}</div></CardHeader><CardContent className="grid gap-5 sm:grid-cols-2"><Info label="Brand" value={item.brand || "Not set"} /><Info label="Category" value={categoryName} /><Info label="Expiry date" value={formatDateOnly(item.expiryDate)} danger={isExpired(item.expiryDate)} /><Info label="Variants" value={`${item.variants.length} configured`} /><Info label="Images" value={`${item.images.length} uploaded`} /></CardContent></Card><Card><CardHeader><CardTitle>Catalog readiness</CardTitle><p className="mt-1 text-sm text-muted-foreground">Quick checks for this product.</p></CardHeader><CardContent className="space-y-3"><Readiness label="Product details" ready={Boolean(item.name && item.categoryId)} /><Readiness label="Sellable variant" ready={item.variants.length > 0} /><Readiness label="Product imagery" ready={item.images.length > 0} /><Readiness label="Active status" ready={item.status === "ACTIVE"} /></CardContent></Card></div><div className="mt-6"><Card><CardHeader><div className="flex items-center gap-2"><Layers3 className="h-5 w-5 text-primary" /><div><CardTitle>Variants</CardTitle><p className="mt-1 text-sm text-muted-foreground">SKU-level pricing and attributes for this product.</p></div></div></CardHeader><CardContent className="p-0"><div className="overflow-x-auto"><table className="w-full min-w-[640px] text-left"><thead><tr className="border-y border-slate-100 text-xs font-semibold uppercase tracking-[0.1em] text-slate-400"><th className="px-5 py-3">SKU</th><th className="px-3 py-3">Price</th><th className="px-3 py-3">Attributes</th><th className="px-3 py-3">Status</th></tr></thead><tbody>{item.variants.map((variant) => <tr className="table-row" key={variant.id}><td className="px-5 py-4 font-mono text-sm font-semibold text-slate-700">{variant.sku}</td><td className="px-3 py-4 text-sm text-slate-600">{formatCurrency(variant.price, variant.currency)}</td><td className="px-3 py-4"><div className="flex flex-wrap gap-1.5">{Object.entries(variant.attributes ?? {}).map(([key, value]) => <span key={key} className="rounded-lg bg-slate-100 px-2 py-1 text-xs text-slate-600">{key}: {value}</span>)}{Object.keys(variant.attributes ?? {}).length === 0 && <span className="text-xs text-slate-400">No attributes</span>}</div></td><td className="px-3 py-4"><StatusBadge status={variant.status} /></td></tr>)}</tbody></table></div></CardContent></Card></div><div className="mt-6"><ProductInventoryPanel variants={item.variants} /></div><div className="mt-6"><ImageManager product={item} /></div><Dialog open={deleteOpen} onOpenChange={setDeleteOpen}><DialogContent><DialogHeader><DialogTitle>Delete {item.name}?</DialogTitle><DialogDescription>This removes the product, its variants and its uploaded images. This action cannot be undone.</DialogDescription></DialogHeader>{remove.error && <div className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2.5 text-sm text-rose-700">{remove.error instanceof ApiError ? remove.error.message : "Unable to delete the product."}</div>}<div className="flex justify-end gap-2"><Button variant="outline" onClick={() => setDeleteOpen(false)}>Cancel</Button><Button variant="destructive" onClick={deleteProduct} disabled={remove.isPending}>{remove.isPending ? "Deleting…" : "Delete product"}</Button></div></DialogContent></Dialog></>}</>;
}

function Info({ label, value, danger = false }: { label: string; value: string; danger?: boolean }) { return <div><p className="text-xs font-semibold uppercase tracking-[0.1em] text-slate-400">{label}</p><p className={`mt-2 text-sm font-semibold ${danger ? "text-rose-700" : "text-slate-800"}`}>{value}</p></div>; }
function Readiness({ label, ready }: { label: string; ready: boolean }) { return <div className="flex items-center justify-between rounded-xl bg-slate-50 px-3 py-2.5"><span className="text-sm text-slate-600">{label}</span><span className={`h-2.5 w-2.5 rounded-full ${ready ? "bg-emerald-500" : "bg-slate-300"}`} aria-label={ready ? "Ready" : "Needs attention"} /></div>; }
