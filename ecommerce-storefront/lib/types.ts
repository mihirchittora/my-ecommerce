export type ProductStatus = "DRAFT" | "ACTIVE" | "INACTIVE";
export type VariantStatus = "ACTIVE" | "INACTIVE";
export type CategoryStatus = "ACTIVE" | "INACTIVE";
export type AddressType = "SHIPPING" | "BILLING";
export type PaymentMethod = "ONLINE" | "CASH_ON_DELIVERY";

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
  tokenType: string;
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

export interface ProductVariant {
  id: string;
  sku: string;
  price: number;
  currency: string;
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

export interface CatalogFacets {
  price: { min: number | null; max: number | null };
  brands: string[];
  attributes: Record<string, string[]>;
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

export interface CartProduct {
  productId: string | null;
  variantId: string | null;
  name: string | null;
  variant: string | null;
  attributes: Record<string, string>;
}

export interface CartPricing {
  unitPrice: number;
  currency: string;
  subtotalEstimate: number;
}

export interface CartAvailability {
  known: boolean;
  availableQuantity: number | null;
  message: string | null;
}

export interface CartItem {
  id: string;
  sku: string;
  quantity: number;
  createdAt: string;
  updatedAt: string;
  product: CartProduct | null;
  pricing: CartPricing | null;
  availability: CartAvailability | null;
  unavailable: boolean;
}

export type CartStatus = "ACTIVE" | "CHECKOUT_IN_PROGRESS" | "CONVERTED" | "ABANDONED" | "EXPIRED";

export interface Cart {
  id: string;
  customerId: string;
  status: CartStatus;
  currency: string;
  createdAt: string;
  updatedAt: string;
  expiresAt: string | null;
  convertedOrderId: string | null;
  convertedOrderNumber: string | null;
  version: number;
  itemCount: number;
  totalQuantity: number;
  enrichmentAvailable: boolean;
  warnings: string[];
  items: CartItem[];
}

export interface CustomerProfile {
  id: string;
  authUserId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  status: "ACTIVE" | "INACTIVE" | "BLOCKED";
  createdAt: string;
  updatedAt: string;
}

export interface Address {
  id: string;
  customerId: string;
  addressType: AddressType;
  recipientName: string;
  phone: string;
  line1: string;
  line2: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  landmark: string | null;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AddressPayload {
  addressType: AddressType;
  recipientName: string;
  phone: string;
  line1: string;
  line2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  landmark?: string;
  isDefault?: boolean;
}

export type OrderStatus = "DRAFT" | "PENDING_RESERVATION" | "RESERVED" | "PENDING_PAYMENT" | "PAID" | "CONFIRMED" | "FULFILLING" | "SHIPPED" | "DELIVERED" | "COMPLETED" | "CANCELLED" | "FAILED";

export interface OrderSummary {
  id: string;
  orderNumber: string;
  customerId: string;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  currency: string;
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
}

export interface OrderItem {
  id: string;
  orderId: string;
  sku: string;
  productNameSnapshot: string;
  variantSnapshot: Record<string, unknown>;
  unitPrice: number;
  currency: string;
  quantity: number;
  subtotal: number;
  createdAt: string;
  reservationId: string | null;
  reservationReference: string | null;
  inventoryUnitIds: string[];
  inventoryUnits: Array<{ id: string; unitCode: string | null }>;
}

export interface OrderAddress {
  sourceAddressId: string | null;
  recipientName: string;
  phone: string;
  line1: string;
  line2: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  landmark: string | null;
}

export interface OrderHistoryEntry {
  id: string;
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus;
  eventType: string;
  referenceId: string | null;
  notes: string | null;
  actorUserId: string | null;
  createdAt: string;
}

export interface OrderDetail extends OrderSummary {
  subtotal: number;
  discountAmount: number;
  shippingAmount: number;
  taxAmount: number;
  cancelledAt: string | null;
  completedAt: string | null;
  shippingAddress: OrderAddress | null;
  items: OrderItem[];
  history: OrderHistoryEntry[];
}

export type PaymentStatus = "CREATED" | "PENDING" | "AUTHORIZED" | "CAPTURED" | "PENDING_COLLECTION" | "FAILED" | "CANCELLED" | "REFUND_PENDING" | "PARTIALLY_REFUNDED" | "REFUNDED";

export interface Payment {
  id: string;
  orderId: string;
  customerId: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  paymentMethod: PaymentMethod;
  provider: string | null;
  providerPaymentId: string | null;
  providerOrderId: string | null;
  checkoutUrl: string | null;
  checkoutToken: string | null;
  refundedAmount: number;
  createdAt: string;
  updatedAt: string;
  authorizedAt: string | null;
  capturedAt: string | null;
  failedAt: string | null;
  cancelledAt: string | null;
  attempts: PaymentAttempt[];
  refunds: PaymentRefund[];
}

export interface PaymentAttempt {
  id: string;
  attemptNumber: number;
  provider: string;
  status: string;
  providerPaymentId: string | null;
  providerOrderId: string | null;
  failureCode: string | null;
  failureMessage: string | null;
  amount: number;
  currency: string;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface PaymentRefund {
  id: string;
  amount: number;
  currency: string;
  status: string;
  providerRefundId: string | null;
  idempotencyKey: string | null;
  reason: string | null;
  failureCode: string | null;
  failureMessage: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
}

export type ShipmentStatus = "CREATED" | "READY" | "PACKED" | "SHIPPED" | "IN_TRANSIT" | "OUT_FOR_DELIVERY" | "DELIVERED" | "DELIVERY_FAILED" | "RETURNED" | "CANCELLED" | "FAILED";

export interface ShipmentSummary {
  id: string;
  shipmentNumber: string;
  fulfillmentId: string;
  orderId: string;
  orderNumber: string;
  status: ShipmentStatus;
  carrier: string;
  serviceLevel: string;
  trackingNumber: string | null;
  currency: string;
  packageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface TrackingResponse {
  shipmentNumber: string;
  carrier: string;
  trackingNumber: string | null;
  currentStatus: ShipmentStatus;
  estimatedDeliveryAt: string | null;
  events: TrackingEvent[];
}

export interface TrackingEvent {
  id: string;
  trackingNumber: string | null;
  carrier: string;
  eventType: string;
  eventStatus: string;
  eventLocation: string | null;
  description: string | null;
  occurredAt: string;
  receivedAt: string | null;
}

export interface Availability {
  sku: string;
  available: boolean;
  message: string;
}

export interface CheckoutResponse {
  cartId: string;
  cartStatus: CartStatus;
  orderId: string;
  orderNumber: string;
  orderStatus: OrderStatus | null;
  message: string;
}

export interface PaymentMethodOption {
  paymentMethod: PaymentMethod;
  eligible: boolean;
  reason: string | null;
}

export interface PaymentMethodOptionsResponse {
  methods: PaymentMethodOption[];
}

export interface ApiErrorResponse {
  timestamp?: string;
  status?: number;
  code?: string;
  message?: string;
  details?: string[];
  fieldErrors?: Record<string, string>;
  path?: string;
}
