import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export type ServiceName = "auth" | "catalog" | "inventory" | "order" | "cart" | "customer" | "payment" | "shipping";

const apiOrigins: Record<ServiceName, string> = {
  auth: process.env.NEXT_PUBLIC_AUTH_API_URL ?? "http://localhost:8085",
  catalog: process.env.NEXT_PUBLIC_CATALOG_API_URL ?? "http://localhost:8081",
  inventory: process.env.NEXT_PUBLIC_INVENTORY_API_URL ?? "http://localhost:8082",
  order: process.env.NEXT_PUBLIC_ORDER_API_URL ?? "http://localhost:8083",
  cart: process.env.NEXT_PUBLIC_CART_API_URL ?? "http://localhost:8084",
  customer: process.env.NEXT_PUBLIC_CUSTOMER_API_URL ?? "http://localhost:8086",
  payment: process.env.NEXT_PUBLIC_PAYMENT_API_URL ?? "http://localhost:8087",
  shipping: process.env.NEXT_PUBLIC_SHIPPING_API_URL ?? "http://localhost:8088",
};

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function getApiBaseUrl(service: ServiceName) {
  if (typeof window === "undefined") return `${apiOrigins[service]}/api`;
  return `/backend/${service}`;
}

export function getAssetUrl(url: string | null | undefined) {
  if (!url) return null;
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  if (url.startsWith("/api/")) return `${getApiBaseUrl("catalog")}${url.slice(4)}`;
  return url;
}

export function formatCurrency(value: number | null | undefined, currency: string) {
  if (value === null || value === undefined || Number.isNaN(value)) return "—";
  try {
    return new Intl.NumberFormat("en-IN", { style: "currency", currency, maximumFractionDigits: 2 }).format(value);
  } catch {
    return `${currency} ${value.toFixed(2)}`;
  }
}

export function formatDate(value: string | null | undefined, withTime = false) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("en-IN", withTime ? { dateStyle: "medium", timeStyle: "short" } : { dateStyle: "medium" }).format(date);
}

export function titleCase(value: string) {
  return value.toLowerCase().replace(/(^|[_-])([a-z])/g, (_, prefix: string, letter: string) => `${prefix}${letter.toUpperCase()}`).replaceAll("_", " ");
}

export function slugify(value: string) {
  return value.toLowerCase().trim().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
}

export function newIdempotencyKey(prefix: string) {
  const random = typeof crypto !== "undefined" && "randomUUID" in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  return `${prefix}-${random}`;
}

export function variantLabel(attributes: Record<string, string>) {
  return Object.entries(attributes).map(([key, value]) => `${humanizeCatalogValue(key)}: ${humanizeCatalogValue(value)}`).join(" · ");
}

export function humanizeCatalogValue(value: string) {
  return value
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
    .replace(/(\d+)(G|GB|ML|L)\b/i, "$1 $2")
    .replace(/\bGb\b/g, "GB")
    .replace(/\bG\b/g, "g")
    .replace(/\bMl\b/g, "ml");
}

export function friendlyError(status: number, fallback = "Something went wrong. Please try again.") {
  if (status === 400) return "Please check the information and try again.";
  if (status === 401) return "Please sign in to continue.";
  if (status === 403) return "You do not have permission to perform this action.";
  if (status === 404) return "We could not find what you were looking for.";
  if (status === 409) return "This request conflicts with the latest information. Please review and try again.";
  if (status === 429) return "Too many requests. Please wait a moment and try again.";
  if (status >= 500) return "The service is temporarily unavailable. Please try again shortly.";
  return fallback;
}
