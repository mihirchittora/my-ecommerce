"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { categoryApi, fetchCategoryTree } from "@/lib/api/categories";
import { imageApi } from "@/lib/api/images";
import { productApi } from "@/lib/api/products";
import type { CategoryPayload, ProductListParams, ProductPayload } from "@/lib/types";

export const queryKeys = {
  categories: ["categories"] as const,
  categoryTree: ["categories", "tree"] as const,
  products: ["products"] as const,
  product: (id: string) => ["products", id] as const,
};

export function useCategoryTree() {
  return useQuery({ queryKey: queryKeys.categoryTree, queryFn: fetchCategoryTree });
}

export function useProductList(params: ProductListParams) {
  return useQuery({ queryKey: [...queryKeys.products, params], queryFn: () => productApi.list(params), placeholderData: (previous) => previous });
}

export function useProduct(id: string) {
  return useQuery({ queryKey: queryKeys.product(id), queryFn: () => productApi.get(id), enabled: Boolean(id) });
}

export function useCreateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CategoryPayload) => categoryApi.create(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.categoryTree }),
  });
}

export function useUpdateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: CategoryPayload }) => categoryApi.update(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.categoryTree }),
  });
}

export function useDeleteCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => categoryApi.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.categoryTree }),
  });
}

export function useCreateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ProductPayload) => productApi.create(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.products }),
  });
}

export function useUpdateProduct(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ProductPayload) => productApi.update(id, payload),
    onSuccess: (product) => {
      queryClient.setQueryData(queryKeys.product(id), product);
      queryClient.invalidateQueries({ queryKey: queryKeys.products });
    },
  });
}

export function useDeleteProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => productApi.delete(id),
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: queryKeys.product(id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.products });
    },
  });
}

export function useUploadImage(productId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ file, sortOrder, variantId }: { file: File; sortOrder: number; variantId?: string }) => imageApi.upload(productId, file, sortOrder, variantId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.product(productId) }),
  });
}

export function useDeleteImage(productId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (imageId: string) => imageApi.delete(productId, imageId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.product(productId) }),
  });
}
