import type { AuthTokenResponse } from "@/lib/types";

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
}

const storageKey = "meridian.admin.auth.session";
let memorySession: AuthSession | null = null;

function toSession(response: AuthTokenResponse): AuthSession {
  return {
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    expiresAt: Date.now() + response.expiresIn * 1000,
  };
}

export function getAuthSession(): AuthSession | null {
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

export function saveAuthSession(response: AuthTokenResponse): AuthSession {
  const session = toSession(response);
  memorySession = session;
  if (typeof window !== "undefined") {
    try {
      window.sessionStorage.setItem(storageKey, JSON.stringify(session));
    } catch {
      // Keep the in-memory session when browser storage is unavailable.
    }
  }
  return session;
}

export function clearAuthSession() {
  memorySession = null;
  if (typeof window !== "undefined") {
    try {
      window.sessionStorage.removeItem(storageKey);
    } catch {
      // Ignore storage failures during logout.
    }
  }
}
