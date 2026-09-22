import type { AuthTokenResponse } from "@/lib/types";

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
}

const storageKey = "ecommerce.storefront.auth.session";
let memorySession: AuthSession | null = null;

function toSession(response: AuthTokenResponse): AuthSession {
  return { accessToken: response.accessToken, refreshToken: response.refreshToken, expiresAt: Date.now() + response.expiresIn * 1000 };
}

export function getAuthSession() {
  if (memorySession) return memorySession;
  if (typeof window === "undefined") return null;
  try {
    const stored = window.sessionStorage.getItem(storageKey);
    memorySession = stored ? JSON.parse(stored) as AuthSession : null;
  } catch {
    memorySession = null;
  }
  return memorySession;
}

export function saveAuthSession(response: AuthTokenResponse) {
  const session = toSession(response);
  memorySession = session;
  if (typeof window !== "undefined") {
    try { window.sessionStorage.setItem(storageKey, JSON.stringify(session)); } catch { /* memory-only session is still usable */ }
  }
  return session;
}

export function clearAuthSession() {
  memorySession = null;
  if (typeof window !== "undefined") {
    try { window.sessionStorage.removeItem(storageKey); } catch { /* ignore storage failures during logout */ }
  }
}
