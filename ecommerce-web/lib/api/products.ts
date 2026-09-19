import { jsonBody, request } from "@/lib/api/client";
import type { PageResponse, Product, ProductListParams, ProductPayload } from "@/lib/types";

export const productApi = {
  list: (params: ProductListParams) => {
    const query = new URLSearchParams({ page: String(params.page), size: String(params.size), sort: params.sort });
    if (params.search) query.set("search", params.search);
    if (params.categoryId) query.set("categoryId", params.categoryId);
    return request<PageResponse<Product>>(`/v1/products?${query.toString()}`);
  },
  get: (id: string) => request<Product>(`/v1/products/${id}`),
  create: (payload: ProductPayload) => request<Product>("/v1/products", { method: "POST", body: jsonBody(payload) }),
  update: (id: string, payload: ProductPayload) => request<Product>(`/v1/products/${id}`, { method: "PUT", body: jsonBody(payload) }),
  delete: (id: string) => request<void>(`/v1/products/${id}`, { method: "DELETE" }),
};
