"use client";

import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { authApi } from "@/lib/api/auth";
import { clearAuthSession, getAuthSession, saveAuthSession } from "@/lib/auth-session";
import type { AuthUser } from "@/lib/types";

export type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  user: AuthUser | null;
  status: AuthStatus;
  login: (email: string, password: string) => Promise<AuthUser>;
  register: (payload: { email: string; password: string; firstName: string; lastName: string }) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");

  useEffect(() => {
    let active = true;
    const expire = () => { if (active) { setUser(null); setStatus("unauthenticated"); } };
    window.addEventListener("auth:session-expired", expire);
    const session = getAuthSession();
    if (!session) {
      setStatus("unauthenticated");
      return () => { active = false; window.removeEventListener("auth:session-expired", expire); };
    }
    authApi.me().then((nextUser) => {
      if (!active) return;
      setUser(nextUser);
      setStatus("authenticated");
    }).catch(() => {
      if (!active) return;
      clearAuthSession();
      setUser(null);
      setStatus("unauthenticated");
    });
    return () => { active = false; window.removeEventListener("auth:session-expired", expire); };
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    status,
    login: async (email, password) => {
      const tokens = await authApi.login({ email, password });
      saveAuthSession(tokens);
      const nextUser = await authApi.me();
      setUser(nextUser);
      setStatus("authenticated");
      return nextUser;
    },
    register: async (payload) => {
      await authApi.register(payload);
      await (async () => {
        const tokens = await authApi.login({ email: payload.email, password: payload.password });
        saveAuthSession(tokens);
        const nextUser = await authApi.me();
        setUser(nextUser);
        setStatus("authenticated");
      })();
    },
    logout: async () => {
      const refreshToken = getAuthSession()?.refreshToken;
      try { if (refreshToken) await authApi.logout(refreshToken); } finally {
        clearAuthSession();
        setUser(null);
        setStatus("unauthenticated");
        queryClient.clear();
      }
    },
  }), [queryClient, status, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used inside AuthProvider");
  return value;
}
