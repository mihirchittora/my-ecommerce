import { request } from "@/lib/api/client";
import type { ImageUploadResponse } from "@/lib/types";

export const imageApi = {
  upload: (productId: string, file: File, sortOrder: number, variantId?: string) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("sortOrder", String(sortOrder));
    if (variantId) formData.append("variantId", variantId);
    return request<ImageUploadResponse>(`/v1/products/${productId}/images`, { method: "POST", body: formData });
  },
  delete: (productId: string, imageId: string) => request<void>(`/v1/products/${productId}/images/${imageId}`, { method: "DELETE" }),
};
