import type { ApiErrorResponse } from "@/lib/types";
import { getApiBaseUrl } from "@/lib/utils";

export class ApiError extends Error {
  status: number;
  details: string[];
  fieldErrors: Record<string, string>;

  constructor(status: number, message: string, details: string[] = [], fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
    this.fieldErrors = fieldErrors;
  }
}

function getFallbackMessage(status: number, path: string) {
  if (status === 400) return "Please correct the highlighted fields.";
  if (status === 404) return path.includes("products") ? "Product not found." : "Category not found.";
  if (status === 409) return "This change conflicts with existing catalog data.";
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

export async function request<T>(path: string, options: RequestInit = {}) {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  const isFormData = options.body instanceof FormData;
  if (options.body && !isFormData) headers.set("Content-Type", "application/json");

  let response: Response;
  try {
    response = await fetch(`${getApiBaseUrl()}${path}`, { ...options, headers });
  } catch {
    throw new ApiError(0, "The catalog service is unavailable. Check that the backend is running on port 8080.");
  }

  const body = (await parseResponse(response)) as ApiErrorResponse | T | undefined;
  if (!response.ok) {
    const errorBody = (body ?? {}) as ApiErrorResponse;
    const serverMessage = typeof errorBody.message === "string" ? errorBody.message : "";
    const message = response.status === 400
      ? "Please correct the highlighted fields."
      : response.status === 404
        ? getFallbackMessage(404, path)
        : response.status >= 500
          ? "Something went wrong. Please try again."
          : serverMessage || getFallbackMessage(response.status, path);
    throw new ApiError(response.status, message, errorBody.details ?? [], parseFieldErrors(errorBody.details));
  }
  return body as T;
}

export function jsonBody<T>(payload: T) {
  return JSON.stringify(payload);
}
