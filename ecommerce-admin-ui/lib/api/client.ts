import type { ApiErrorResponse } from "@/lib/types";
import { clearAuthSession, getAuthSession, saveAuthSession } from "@/lib/auth-session";
import { getApiBaseUrl, type ServiceName } from "@/lib/utils";

export class ApiError extends Error {
  status: number;
  service: ServiceName;
  details: string[];
  fieldErrors: Record<string, string>;

  constructor(status: number, message: string, details: string[] = [], fieldErrors: Record<string, string> = {}, service: ServiceName = "catalog") {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.service = service;
    this.details = details;
    this.fieldErrors = fieldErrors;
  }
}

function getFallbackMessage(status: number, path: string, service: ServiceName) {
  if (status === 400) return "Please correct the highlighted fields.";
  if (service === "auth" && status === 404) return "Authentication service resource not found.";
  if (status === 404) {
    if (service === "catalog") return path.includes("products") ? "Product not found." : "Category not found.";
    if (service === "inventory") return "Inventory resource not found.";
    if (service === "order") return "Order not found.";
    if (service === "cart") return "Cart not found.";
    if (service === "customer") return "Customer resource not found.";
    if (service === "payment") return "Payment not found.";
    if (service === "shipping") return path.includes("fulfillment") ? "Fulfillment not found." : "Shipment not found.";
  }
  if (status === 409) return service === "order" ? "Order cannot be changed in its current state." : service === "cart" ? "Cart cannot be read in its current state." : "This change conflicts with existing catalog data.";
    if (status >= 500) return "Something went wrong. Please try again.";
  return "Unable to complete the request.";
}

function parseFieldErrors(details: string[] | undefined) {
  return (details ?? []).reduce<Record<string, string>>((errors, detail) => {
    const separator = detail.indexOf(":");
    if (separator > 0) errors[detail.slice(0, separator).trim()] = detail.slice(separator + 1).trim();
    return errors;
  }, {});
}

async function parseResponse(response: Response): Promise<unknown> {
  const contentType = response.headers.get("content-type") ?? "";
  if (response.status === 204) return undefined;
  if (contentType.includes("application/json")) return response.json();
  const text = await response.text();
  return text ? { message: text } : undefined;
}

export async function requestForService<T>(service: ServiceName, path: string, options: RequestInit = {}) {
  const isFormData = options.body instanceof FormData;
  const canRefresh = !path.endsWith("/login") && !path.endsWith("/refresh") && !path.endsWith("/register");
  let response: Response | undefined;
  let refreshed = false;

  do {
    const headers = new Headers(options.headers);
    headers.set("Accept", "application/json");
    if (options.body && !isFormData) headers.set("Content-Type", "application/json");
    const session = getAuthSession();
    if (session?.accessToken && !headers.has("Authorization")) headers.set("Authorization", `Bearer ${session.accessToken}`);

    try {
      response = await fetch(`${getApiBaseUrl(service)}${path}`, { ...options, headers });
    } catch {
      const label = service === "auth" ? "Authentication" : service === "catalog" ? "Catalog" : service === "inventory" ? "Inventory" : service === "order" ? "Order" : service === "cart" ? "Cart" : service === "customer" ? "Customer" : service === "payment" ? "Payment" : "Shipping";
      throw new ApiError(0, `${label} service is unavailable. Check that it is running.`, [], {}, service);
    }

    if (response.status !== 401 || refreshed || !canRefresh || !getAuthSession()?.refreshToken) break;
    refreshed = true;
    try {
      const refreshResponse = await fetch(`${getApiBaseUrl("auth")}/v1/auth/refresh`, {
        method: "POST",
        headers: { Accept: "application/json", "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: getAuthSession()?.refreshToken }),
      });
      if (!refreshResponse.ok) {
        clearAuthSession();
        if (typeof window !== "undefined") window.dispatchEvent(new Event("auth:session-expired"));
        break;
      }
      saveAuthSession(await refreshResponse.json());
    } catch {
      clearAuthSession();
      if (typeof window !== "undefined") window.dispatchEvent(new Event("auth:session-expired"));
      break;
    }
  } while (refreshed && response?.status === 401);

  if (!response) throw new ApiError(0, "Unable to reach the requested service.", [], {}, service);

  const body = (await parseResponse(response)) as ApiErrorResponse | T | undefined;
  if (!response.ok) {
    const errorBody = (body ?? {}) as ApiErrorResponse;
    const serverMessage = typeof errorBody.message === "string" ? errorBody.message : "";
    const message = response.status === 400
      ? "Please correct the highlighted fields."
      : response.status === 404
          ? getFallbackMessage(404, path, service)
        : response.status >= 500
          ? "Something went wrong. Please try again."
          : serverMessage || getFallbackMessage(response.status, path, service);
    throw new ApiError(response.status, message, errorBody.details ?? [], parseFieldErrors(errorBody.details), service);
  }
  return body as T;
}

export function createApiClient(service: ServiceName) {
  return {
    request: <T>(path: string, options?: RequestInit) => requestForService<T>(service, path, options),
    json: <TResponse, TPayload = unknown>(path: string, payload: TPayload, options: Omit<RequestInit, "body"> = {}) => requestForService<TResponse>(service, path, { ...options, method: options.method ?? "POST", body: JSON.stringify(payload) }),
  };
}

export const catalogClient = createApiClient("catalog");
export const inventoryClient = createApiClient("inventory");
export const authClient = createApiClient("auth");
export const orderClient = createApiClient("order");
export const cartClient = createApiClient("cart");
export const customerClient = createApiClient("customer");
export const paymentClient = createApiClient("payment");
export const shippingClient = createApiClient("shipping");

// Backwards-compatible catalog request helper for the existing catalog modules.
export function request<T>(path: string, options: RequestInit = {}) {
  return requestForService<T>("catalog", path, options);
}

export function jsonBody<T>(payload: T) {
  return JSON.stringify(payload);
}
