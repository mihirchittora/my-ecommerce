export type CategoryStatus = "ACTIVE" | "INACTIVE";
export type ProductStatus = "DRAFT" | "ACTIVE" | "INACTIVE";
export type VariantStatus = "ACTIVE" | "INACTIVE";
export type CurrencyCode = "INR" | "USD" | "EUR" | "GBP" | "JPY" | "AUD" | "CAD" | "SGD";

export interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  permissions: string[];
}

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer" | string;
  expiresIn: number;
}

export interface Category {
  id: string;
  parentId: string | null;
  name: string;
  slug: string;
  status: CategoryStatus;
  description: string | null;
  image: CategoryImage | null;
}

export interface CategoryImage {
  id: string;
  url: string;
  altText: string;
}

export interface CategoryNode extends Category {
  children: CategoryNode[];
}

export interface SiteSettings {
  id: string;
  siteTitle: string;
  logoUrl: string | null;
  logoOriginalFilename: string | null;
  logoContentType: string | null;
  logoSizeBytes: number | null;
  updatedAt: string;
  slides: CarouselSlide[];
}

export interface CarouselSlide {
  id: string;
  sortOrder: number;
  eyebrow: string | null;
  headline: string;
  description: string | null;
  primaryCtaLabel: string | null;
  primaryCtaUrl: string | null;
  secondaryCtaLabel: string | null;
  secondaryCtaUrl: string | null;
  imageUrl: string | null;
  active: boolean;
  originalFilename: string | null;
  contentType: string | null;
  sizeBytes: number | null;
  updatedAt: string;
}

export interface ProductVariant {
  id: string;
  sku: string;
  price: number;
  taxRate: number;
  taxAmount: number;
  priceIncludingTax: number;
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
  expiryDate: string | null;
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
  path?: string;
}

export interface CategoryPayload {
  name: string;
  parentId?: string | null;
  slug?: string;
  description?: string | null;
  status?: CategoryStatus;
}

export interface VariantPayload {
  sku: string;
  price: number;
  taxRate: number;
  currency: CurrencyCode;
  attributes: Record<string, string>;
  status: VariantStatus;
}

export interface ProductPayload {
  categoryId: string;
  name: string;
  description?: string;
  brand?: string;
  expiryDate?: string;
  status?: ProductStatus;
  variants: VariantPayload[];
}

export interface ProductListParams {
  page: number;
  size: number;
  sort: string;
  search?: string;
  categoryId?: string;
  status?: ProductStatus;
}

export type ImageUploadResponse = ProductImage;

export type LocationStatus = "ACTIVE" | "INACTIVE";
export type InventoryUnitStatus = "AVAILABLE" | "RESERVED" | "ALLOCATED" | "IN_TRANSIT" | "SOLD" | "RETURNED" | "DAMAGED" | "LOST";
export type ReservationStatus = "ACTIVE" | "CONFIRMED" | "RELEASED" | "EXPIRED" | "CANCELLED";
export type AdjustmentReason = "PURCHASE_RECEIPT" | "MANUAL_CORRECTION" | "DAMAGE" | "LOSS" | "FOUND" | "RETURN" | "INITIAL_STOCK";

export interface InventoryLocation {
  id: string;
  code: string;
  name: string;
  status: LocationStatus;
  inventoryCount?: number;
}

export interface InventoryItem {
  id: string;
  sku: string;
  location: InventoryLocation;
  locationId?: string;
  quantity: number;
  reservedQuantity: number;
  available: number;
  version?: number;
}

export interface InventoryUnit {
  id: string;
  inventoryItemId?: string;
  unitCode: string;
  sku: string;
  productName?: string | null;
  location: InventoryLocation;
  locationId?: string;
  status: InventoryUnitStatus;
  serialNumber: string | null;
  imei: string | null;
  barcode: string | null;
  receivedAt: string;
  soldAt: string | null;
}

export interface InventoryReservation {
  id: string;
  sku: string;
  location: InventoryLocation;
  quantity: number;
  referenceId: string;
  status: ReservationStatus;
  expiresAt: string | null;
  units: InventoryUnit[];
  createdAt?: string;
}

export interface InventoryAdjustment {
  id: string;
  sku: string;
  location: InventoryLocation;
  quantity: number;
  reason: AdjustmentReason;
  referenceId: string | null;
  unitIds?: string[];
  createdAt: string;
}

export interface InventoryMovement {
  id: string;
  inventoryUnitId: string;
  fromLocation: InventoryLocation | null;
  toLocation: InventoryLocation | null;
  fromStatus: InventoryUnitStatus | null;
  toStatus: InventoryUnitStatus | null;
  referenceType: string;
  referenceId: string | null;
  notes: string | null;
  createdAt: string;
}

export interface InventoryTransfer {
  id: string;
  sku: string;
  fromLocation: InventoryLocation;
  toLocation: InventoryLocation;
  unitIds: string[];
  referenceId: string | null;
  createdAt: string;
}

export interface InventoryStockRow {
  sku: string;
  productName?: string | null;
  location?: InventoryLocation | null;
  locationCount: number;
  totalUnits: number;
  available: number;
  reserved: number;
  damaged: number;
  status?: string;
}

export interface InventorySummary {
  totalInventoryUnits: number;
  availableUnits: number;
  reservedUnits: number;
  damagedUnits: number;
  lowStockSkus: number;
  activeLocations: number;
}

export interface InventoryReconciliationRow {
  sku: string;
  location: InventoryLocation;
  expected: number;
  actual: number;
  difference: number;
  status: "CONSISTENT" | "DISCREPANCY";
}

export interface InventorySkuSummary {
  sku: string;
  totalQuantity: number;
  totalReserved: number;
  totalAvailable: number;
  locations: Array<{
    locationId: string;
    locationCode: string;
    locationName: string;
    quantity: number;
    reservedQuantity: number;
    available: number;
  }>;
}

export interface InventoryReceiveUnitPayload {
  serialNumber?: string;
  imei?: string;
  barcode?: string;
}

export interface InventoryReceivePayload {
  sku: string;
  locationId: string;
  quantity: number;
  referenceId: string;
  units: InventoryReceiveUnitPayload[];
}

export interface InventoryReservationPayload {
  sku: string;
  locationId: string;
  quantity: number;
  referenceId: string;
  expiresAt?: string;
}

export interface InventoryAdjustmentPayload {
  sku: string;
  locationId: string;
  reason: AdjustmentReason;
  quantity: number;
  referenceId?: string;
  unitIds?: string[];
  units?: InventoryReceiveUnitPayload[];
}

export interface InventoryTransferPayload {
  sku: string;
  fromLocationId: string;
  toLocationId: string;
  unitIds: string[];
  referenceId: string;
}
