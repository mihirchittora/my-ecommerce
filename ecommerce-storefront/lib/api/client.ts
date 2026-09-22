import type { ApiErrorResponse, AuthTokenResponse } from "@/lib/types";
import { clearAuthSession, getAuthSession, saveAuthSession } from "@/lib/auth-session";
import { friendlyError, getApiBaseUrl, type ServiceName } from "@/lib/utils";

export class ApiError extends Error {
  constructor(public status: number, message: string, public service: ServiceName, public fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = "ApiError";
  }
}

async function parseResponse(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined;
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) return response.json();
  const text = await response.text();
  return text ? { message: text } : undefined;
}

async function refreshSession() {
  const current = getAuthSession();
  if (!current?.refreshToken) return false;
  try {
    const response = await fetch(`${getApiBaseUrl("auth")}/v1/auth/refresh`, {
      method: "POST",
      headers: { Accept: "application/json", "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: current.refreshToken }),
    });
    if (!response.ok) throw new Error("Refresh failed");
    saveAuthSession(await response.json() as AuthTokenResponse);
    return true;
  } catch {
    clearAuthSession();
    if (typeof window !== "undefined") window.dispatchEvent(new Event("auth:session-expired"));
    return false;
  }
}

export async function requestForService<T>(service: ServiceName, path: string, options: RequestInit = {}) {
  const canRefresh = !path.endsWith("/login") && !path.endsWith("/refresh") && !path.endsWith("/register");
  let attemptedRefresh = false;
  let response: Response;

  while (true) {
    const headers = new Headers(options.headers);
    headers.set("Accept", "application/json");
    if (options.body && !(options.body instanceof FormData)) headers.set("Content-Type", "application/json");
    const session = getAuthSession();
    if (session?.accessToken && !headers.has("Authorization")) headers.set("Authorization", `Bearer ${session.accessToken}`);

    try {
      response = await fetch(`${getApiBaseUrl(service)}${path}`, { ...options, headers, cache: "no-store" });
    } catch {
      throw new ApiError(0, "The service is unavailable. Please try again shortly.", service);
    }

    if (response.status !== 401 || !canRefresh || attemptedRefresh) break;
    attemptedRefresh = true;
    if (!(await refreshSession())) break;
  }

  const body = await parseResponse(response) as ApiErrorResponse | T | undefined;
  if (!response.ok) {
    const error = (body ?? {}) as ApiErrorResponse;
    const message = typeof error.message === "string" && error.message.length > 0 ? error.message : friendlyError(response.status);
    throw new ApiError(response.status, response.status >= 500 ? friendlyError(response.status) : message, service, error.fieldErrors ?? {});
  }
  return body as T;
}

export function createApiClient(service: ServiceName) {
  return {
    request: <T>(path: string, options?: RequestInit) => requestForService<T>(service, path, options),
    json: <TResponse, TPayload>(path: string, payload: TPayload, options: Omit<RequestInit, "body"> = {}) => requestForService<TResponse>(service, path, { ...options, method: options.method ?? "POST", body: JSON.stringify(payload) }),
  };
}

export const authClient = createApiClient("auth");
export const catalogClient = createApiClient("catalog");
export const inventoryClient = createApiClient("inventory");
export const cartClient = createApiClient("cart");
export const customerClient = createApiClient("customer");
export const orderClient = createApiClient("order");
export const paymentClient = createApiClient("payment");
export const shippingClient = createApiClient("shipping");
