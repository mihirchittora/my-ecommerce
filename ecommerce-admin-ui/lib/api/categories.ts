import { jsonBody, request } from "@/lib/api/client";
import type { Category, CategoryImage, CategoryNode, CategoryPayload } from "@/lib/types";

export const categoryApi = {
  list: (parentId?: string) => request<Category[]>(`/v1/categories${parentId ? `?parentId=${encodeURIComponent(parentId)}` : ""}`),
  get: (id: string) => request<Category>(`/v1/categories/${id}`),
  create: (payload: CategoryPayload) => request<Category>("/v1/categories", { method: "POST", body: jsonBody(payload) }),
  update: (id: string, payload: CategoryPayload) => request<Category>(`/v1/categories/${id}`, { method: "PUT", body: jsonBody(payload) }),
  delete: (id: string) => request<void>(`/v1/categories/${id}`, { method: "DELETE" }),
  uploadImage: (id: string, file: File, altText?: string) => {
    const body = new FormData();
    body.append("file", file);
    if (altText?.trim()) body.append("altText", altText.trim());
    return request<CategoryImage>(`/v1/categories/${id}/image`, { method: "POST", body });
  },
  updateImageAltText: (id: string, altText: string) => request<CategoryImage>(`/v1/categories/${id}/image`, { method: "PUT", body: jsonBody({ altText }) }),
  deleteImage: (id: string) => request<void>(`/v1/categories/${id}/image`, { method: "DELETE" }),
};

export async function fetchCategoryTree(): Promise<CategoryNode[]> {
  const roots = await categoryApi.list();
  const build = async (category: Category): Promise<CategoryNode> => {
    const children = await categoryApi.list(category.id);
    return { ...category, children: await Promise.all(children.map(build)) };
  };
  return Promise.all(roots.map(build));
}
