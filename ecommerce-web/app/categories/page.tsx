"use client";

import { Plus, Shapes } from "lucide-react";
import { useState } from "react";
import { CategoryForm } from "@/components/category-form";
import { CategoryTree } from "@/components/category-tree";
import { EmptyState, ErrorState, LoadingCard } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useCreateCategory, useDeleteCategory, useCategoryTree, useUpdateCategory } from "@/lib/queries";
import type { CategoryNode } from "@/lib/types";
import { ApiError } from "@/lib/api/client";
import { useToast } from "@/components/ui/toast";

type DialogMode = { mode: "create" | "edit"; parentId?: string; category?: CategoryNode } | null;

export default function CategoriesPage() {
  const tree = useCategoryTree();
  const create = useCreateCategory();
  const update = useUpdateCategory();
  const remove = useDeleteCategory();
  const { toast } = useToast();
  const [dialog, setDialog] = useState<DialogMode>(null);
  const [deleteTarget, setDeleteTarget] = useState<CategoryNode | null>(null);
  const submit = (values: { name: string; slug?: string; parentId?: string; status: "ACTIVE" | "INACTIVE" }) => {
    const payload = { name: values.name, slug: values.slug || undefined, parentId: values.parentId || null, status: values.status };
    if (dialog?.mode === "edit" && dialog.category) update.mutate({ id: dialog.category.id, payload }, { onSuccess: () => { setDialog(null); toast({ title: "Category updated", description: `${values.name} is ready.` }); } });
    else create.mutate(payload, { onSuccess: () => { setDialog(null); toast({ title: "Category created", description: `${values.name} was added to the hierarchy.` }); } });
  };
  const confirmDelete = () => { if (!deleteTarget) return; remove.mutate(deleteTarget.id, { onSuccess: () => { setDeleteTarget(null); toast({ title: "Category deleted" }); } }); };
  return <><PageIntro eyebrow="Merchandising" title="Categories" description="Shape the hierarchy customers use to browse your catalog." action={{ label: "Add root category", href: "#create" }} /><div className="mb-5 flex items-center justify-between rounded-2xl border border-blue-100 bg-blue-50/70 px-4 py-3 text-sm text-blue-900"><div className="flex items-center gap-3"><Shapes className="h-4 w-4 text-primary" /><span>Root categories are shown first. Use each row’s menu to add child categories, edit, or delete.</span></div><Button size="sm" onClick={() => setDialog({ mode: "create" })}><Plus className="h-4 w-4" />Add category</Button></div>{tree.isLoading ? <LoadingCard rows={6} /> : tree.isError ? <ErrorState onRetry={() => void tree.refetch()} /> : tree.data?.length === 0 ? <EmptyState title="No categories yet" message="Create your first root category to start organizing products." action={<Button onClick={() => setDialog({ mode: "create" })}><Plus className="h-4 w-4" />Create root category</Button>} /> : <Card><CardHeader className="border-b border-slate-100 pb-4"><CardTitle>Category hierarchy <span className="ml-2 text-sm font-normal text-muted-foreground">{tree.data?.length} root categories</span></CardTitle></CardHeader><CardContent className="p-0"><CategoryTree nodes={tree.data ?? []} onEdit={(category) => setDialog({ mode: "edit", category })} onAddChild={(category) => setDialog({ mode: "create", parentId: category.id })} onDelete={setDeleteTarget} /></CardContent></Card>}<Dialog open={Boolean(dialog)} onOpenChange={(open) => !open && setDialog(null)}><DialogContent><DialogHeader><DialogTitle>{dialog?.mode === "edit" ? "Edit category" : dialog?.parentId ? "Add child category" : "Add root category"}</DialogTitle><DialogDescription>{dialog?.mode === "edit" ? "Keep the category name and placement accurate." : "Categories can be nested later without changing product assignments."}</DialogDescription></DialogHeader>{dialog && <CategoryForm categories={tree.data ?? []} parentId={dialog.parentId} initial={dialog.category} onSubmit={submit} onCancel={() => setDialog(null)} isSubmitting={create.isPending || update.isPending} serverError={create.error ?? update.error} />}</DialogContent></Dialog><Dialog open={Boolean(deleteTarget)} onOpenChange={(open) => !open && setDeleteTarget(null)}><DialogContent><DialogHeader><DialogTitle>Delete {deleteTarget?.name}?</DialogTitle><DialogDescription>Categories with child categories or products cannot be deleted. The backend will validate this operation.</DialogDescription></DialogHeader>{remove.error && <div className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2.5 text-sm text-rose-700">{remove.error instanceof ApiError ? remove.error.message : "Unable to delete this category."}</div>}<div className="flex justify-end gap-2"><Button variant="outline" onClick={() => setDeleteTarget(null)}>Cancel</Button><Button variant="destructive" onClick={confirmDelete} disabled={remove.isPending}>{remove.isPending ? "Deleting…" : "Delete category"}</Button></div></DialogContent></Dialog></>;
}
