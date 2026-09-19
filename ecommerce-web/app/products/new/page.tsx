"use client";

import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { PageIntro } from "@/components/page-intro";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { ProductForm } from "@/components/products/product-form";
import { Button } from "@/components/ui/button";
import { useCategoryTree, useCreateProduct } from "@/lib/queries";
import { useToast } from "@/components/ui/toast";

export default function NewProductPage() {
  const router = useRouter();
  const categories = useCategoryTree();
  const create = useCreateProduct();
  const { toast } = useToast();
  return <><div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/products"><ArrowLeft className="h-4 w-4" />Back to products</Link></Button></div><PageIntro eyebrow="Assortment" title="Create product" description="Add the product details and at least one sellable variant. You can upload imagery after saving." />{categories.isLoading ? <LoadingCard rows={5} /> : categories.isError ? <ErrorState onRetry={() => void categories.refetch()} /> : <ProductForm categories={categories.data ?? []} onSubmit={(payload) => create.mutate(payload, { onSuccess: (product) => { toast({ title: "Product created", description: `${product.name} is ready for imagery.` }); router.push(`/products/${product.id}`); } })} isSubmitting={create.isPending} serverError={create.error} submitLabel="Create product" />}</>;
}
