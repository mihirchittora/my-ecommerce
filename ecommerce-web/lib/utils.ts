import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

const apiOrigin = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export function getApiBaseUrl() {
  return typeof window === "undefined" ? `${apiOrigin}/api` : "/backend-api";
}

export function getAssetUrl(url: string | null | undefined) {
  if (!url) return null;
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  if (url.startsWith("/api/")) return `${getApiBaseUrl()}${url.slice(4)}`;
  return url;
}

export function getImageFileUrl(productId: string, imageId: string) {
  return `${getApiBaseUrl()}/v1/products/${productId}/images/${imageId}/file`;
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
