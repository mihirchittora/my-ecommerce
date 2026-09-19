export type CategoryStatus = "ACTIVE" | "INACTIVE";
export type ProductStatus = "DRAFT" | "ACTIVE" | "INACTIVE";
export type VariantStatus = "ACTIVE" | "INACTIVE";
export type CurrencyCode = "INR" | "USD" | "EUR" | "GBP" | "JPY" | "AUD" | "CAD" | "SGD";

export interface Category {
  id: string;
  parentId: string | null;
  name: string;
  slug: string;
  status: CategoryStatus;
}

export interface CategoryNode extends Category {
  children: CategoryNode[];
}

export interface ProductVariant {
  id: string;
  sku: string;
  price: number;
  currency: CurrencyCode;
  attributes: Record<string, string>;
  status: VariantStatus;
}

export interface ProductImage {
  id: string;
  variantId: string | null;
  url: string;
  sortOrder: number;
  originalFilename: string | null;
  contentType: string | null;
  sizeBytes: number | null;
}

export interface Product {
  id: string;
  categoryId: string;
  name: string;
  slug: string;
  description: string | null;
  brand: string | null;
  status: ProductStatus;
  variants: ProductVariant[];
  images: ProductImage[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  details?: string[];
}

export interface CategoryPayload {
  name: string;
  parentId?: string | null;
  slug?: string;
  status?: CategoryStatus;
}

export interface VariantPayload {
  sku: string;
  price: number;
  currency: CurrencyCode;
  attributes: Record<string, string>;
  status: VariantStatus;
}

export interface ProductPayload {
  categoryId: string;
  name: string;
  description?: string;
  brand?: string;
  status?: ProductStatus;
  variants: VariantPayload[];
}

export interface ProductListParams {
  page: number;
  size: number;
  sort: string;
  search?: string;
  categoryId?: string;
}

export type ImageUploadResponse = ProductImage;
