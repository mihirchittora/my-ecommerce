import { catalogClient } from "@/lib/api/client";
import type { CatalogFacets, Category, PageResponse, Product } from "@/lib/types";

function query(values: Record<string, string | number | string[] | undefined>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value === undefined || value === "" || (Array.isArray(value) && value.length === 0)) return;
    if (Array.isArray(value)) value.forEach((item) => params.append(key, item));
    else params.set(key, String(value));
  });
  const text = params.toString();
  return text ? `?${text}` : "";
}

export const catalogApi = {
  listCategories: (parentId?: string) => catalogClient.request<Category[]>(`/v1/categories${query({ parentId })}`),
  getCategoryBySlug: (slug: string) => catalogClient.request<Category>(`/v1/categories/slug/${encodeURIComponent(slug)}`),
  listProducts: (params: { page?: number; size?: number; search?: string; categoryId?: string; sort?: string; priceMin?: number; priceMax?: number; brands?: string[]; attributes?: string[] }) => catalogClient.request<PageResponse<Product>>(`/v1/products${query({ page: params.page ?? 0, size: params.size ?? 12, search: params.search, categoryId: params.categoryId, status: "ACTIVE", sort: params.sort ?? "name,asc", priceMin: params.priceMin, priceMax: params.priceMax, brand: params.brands, attribute: params.attributes })}`),
  getFacets: (params: { search?: string; categoryId?: string }) => catalogClient.request<CatalogFacets>(`/v1/products/facets${query({ search: params.search, categoryId: params.categoryId, status: "ACTIVE" })}`),
  getProductBySlug: (slug: string) => catalogClient.request<Product>(`/v1/products/slug/${encodeURIComponent(slug)}`),
};
