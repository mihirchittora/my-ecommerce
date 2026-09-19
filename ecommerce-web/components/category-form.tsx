"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import type { CategoryNode, CategoryStatus } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { ApiError } from "@/lib/api/client";

const schema = z.object({ name: z.string().trim().min(1, "Category name is required.").max(120, "Use 120 characters or fewer."), slug: z.string().trim().max(150, "Use 150 characters or fewer.").optional(), parentId: z.string().optional(), status: z.enum(["ACTIVE", "INACTIVE"]) });
type Values = z.infer<typeof schema>;

function flatten(nodes: CategoryNode[], depth = 0): Array<{ id: string; label: string; depth: number }> { return nodes.flatMap((node) => [{ id: node.id, label: node.name, depth }, ...flatten(node.children, depth + 1)]); }

export function CategoryForm({ categories, initial, parentId, onSubmit, onCancel, isSubmitting, serverError }: { categories: CategoryNode[]; initial?: { name: string; slug: string; parentId: string | null; status: CategoryStatus }; parentId?: string; onSubmit: (values: Values) => void; onCancel: () => void; isSubmitting?: boolean; serverError?: unknown }) {
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { name: initial?.name ?? "", slug: initial?.slug ?? "", parentId: initial?.parentId ?? parentId ?? "", status: initial?.status ?? "ACTIVE" } });
  const errorMessage = serverError instanceof ApiError ? serverError.message : serverError ? "Please review the category and try again." : undefined;
  const options = flatten(categories).filter((category) => category.id !== initial?.parentId);
  return <form className="space-y-5" onSubmit={form.handleSubmit(onSubmit)} noValidate><div><Label htmlFor="category-name">Name</Label><Input id="category-name" className="mt-2" placeholder="e.g. Audio & headphones" {...form.register("name")} />{form.formState.errors.name && <p className="mt-1.5 text-xs text-rose-600">{form.formState.errors.name.message}</p>}</div><div><Label htmlFor="category-slug">Slug <span className="font-normal text-muted-foreground">(optional)</span></Label><Input id="category-slug" className="mt-2" placeholder="audio-headphones" {...form.register("slug")} />{form.formState.errors.slug && <p className="mt-1.5 text-xs text-rose-600">{form.formState.errors.slug.message}</p>}</div><div><Label htmlFor="category-parent">Parent category</Label><Select id="category-parent" className="mt-2" {...form.register("parentId")}><option value="">Root category</option>{options.map((option) => <option key={option.id} value={option.id}>{"— ".repeat(option.depth)}{option.label}</option>)}</Select></div>{initial && <div><Label htmlFor="category-status">Status</Label><Select id="category-status" className="mt-2" {...form.register("status")}><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></Select></div>}{errorMessage && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2.5 text-sm text-rose-700">{errorMessage}{serverError instanceof ApiError && serverError.details.length > 0 && <ul className="mt-1 list-disc pl-4 text-xs">{serverError.details.map((detail) => <li key={detail}>{detail}</li>)}</ul>}</div>}<div className="flex justify-end gap-2"><Button type="button" variant="outline" onClick={onCancel}>Cancel</Button><Button type="submit" disabled={isSubmitting}>{isSubmitting ? "Saving…" : initial ? "Save changes" : "Create category"}</Button></div></form>;
}
