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
  getProductById: (id: string) => catalogClient.request<Product>(`/v1/products/${encodeURIComponent(id)}`),
  reviews: (productId: string, sku?: string, page = 0) => catalogClient.request<PageResponse<Review>>(`/v1/products/${encodeURIComponent(productId)}/reviews?page=${page}&size=10${sku ? `&sku=${encodeURIComponent(sku)}` : ""}`),
  reviewSummary: (productId: string, sku?: string) => catalogClient.request<ReviewSummary>(`/v1/products/${encodeURIComponent(productId)}/reviews/summary${sku ? `?sku=${encodeURIComponent(sku)}` : ""}`),
  getMyReview: (productId: string, sku?: string, orderItemId?: string) => catalogClient.request<Review | undefined>(`/v1/products/${encodeURIComponent(productId)}/reviews/mine${query({ sku, orderItemId })}`),
  createReview: (productId: string, payload: CreateReviewRequest) => catalogClient.json<Review, CreateReviewRequest>(`/v1/products/${encodeURIComponent(productId)}/reviews`, payload),
  updateReview: (reviewId: string, payload: UpdateReviewRequest) => catalogClient.json<Review, UpdateReviewRequest>(`/v1/reviews/${encodeURIComponent(reviewId)}`, payload, { method: "PUT" }),
};

export interface Review { id: string; productId: string; sku: string | null; orderId: string; rating: number; title: string | null; comment: string; status: string; verifiedPurchase: boolean; createdAt: string; updatedAt: string; }
export interface ReviewSummary { averageRating: number; reviewCount: number; distribution: Record<string, number>; }
export interface CreateReviewRequest { orderId: string; orderItemId: string; sku?: string; rating: number; title?: string; comment: string; }
export interface UpdateReviewRequest { rating: number; title?: string; comment: string; }
