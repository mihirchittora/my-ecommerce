import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export type ServiceName = "auth" | "catalog" | "inventory" | "order" | "cart" | "customer";

const apiOrigins: Record<ServiceName, string> = {
  auth: process.env.NEXT_PUBLIC_AUTH_API_URL ?? "http://localhost:8085",
  catalog: process.env.NEXT_PUBLIC_CATALOG_API_URL ?? "http://localhost:8081",
  inventory: process.env.NEXT_PUBLIC_INVENTORY_API_URL ?? "http://localhost:8082",
  order: process.env.NEXT_PUBLIC_ORDER_API_URL ?? "http://localhost:8083",
  cart: process.env.NEXT_PUBLIC_CART_API_URL ?? "http://localhost:8084",
  customer: process.env.NEXT_PUBLIC_CUSTOMER_API_URL ?? "http://localhost:8086",
};

export function getApiBaseUrl(service: ServiceName = "catalog") {
  if (typeof window === "undefined") return `${apiOrigins[service]}/api`;
  if (service === "auth") return "/backend/auth";
  if (service === "catalog") return "/backend/catalog";
  if (service === "inventory") return "/backend/inventory";
  if (service === "order") return "/backend/order";
  if (service === "cart") return "/backend/cart";
  return "/backend/customer";
}

export function getAssetUrl(url: string | null | undefined) {
  if (!url) return null;
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  if (url.startsWith("/api/")) return `${getApiBaseUrl("catalog")}${url.slice(4)}`;
  return url;
}

export function getImageFileUrl(productId: string, imageId: string) {
  return `${getApiBaseUrl("catalog")}/v1/products/${productId}/images/${imageId}/file`;
}

export function formatCurrency(value: number, currency: string) {
  try {
    return new Intl.NumberFormat("en-IN", { style: "currency", currency, maximumFractionDigits: 2 }).format(value);
  } catch {
    return `${currency} ${value.toFixed(2)}`;
  }
}

export function formatFileSize(bytes: number | null | undefined) {
  if (!bytes) return "—";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function formatDate(value: string | null | undefined) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("en-IN", { dateStyle: "medium", timeStyle: "short" }).format(date);
}

export function formatDateOnly(value: string | null | undefined) {
  if (!value) return "Not set";
  const [year, month, day] = value.split("-").map(Number);
  if (!year || !month || !day) return value;
  return new Intl.DateTimeFormat("en-IN", { dateStyle: "medium", timeZone: "UTC" }).format(new Date(Date.UTC(year, month - 1, day)));
}

export function isExpired(value: string | null | undefined) {
  return Boolean(value && value < new Date().toISOString().slice(0, 10));
}

export function titleCase(value: string) {
  return value.toLowerCase().replace(/(^|[_-])([a-z])/g, (_, prefix: string, letter: string) => `${prefix}${letter.toUpperCase()}`).replaceAll("_", " ");
}

export function debounce<T extends (...args: never[]) => void>(callback: T, delay: number) {
  let timer: ReturnType<typeof setTimeout> | undefined;
  return (...args: Parameters<T>) => {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => callback(...args), delay);
  };
}
