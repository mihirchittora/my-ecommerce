"use client";

import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { authApi } from "@/lib/api/auth";
import { clearAuthSession, getAuthSession, saveAuthSession } from "@/lib/auth-session";
import { hasAllPermissions, hasAnyPermission, hasPermission, isInternalUser, type Permission } from "@/lib/permissions";
import type { AuthUser } from "@/lib/types";

export type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  user: AuthUser | null;
  status: AuthStatus;
  login: (email: string, password: string) => Promise<AuthUser>;
  logout: () => Promise<void>;
  hasPermission: (permission: Permission) => boolean;
  hasAnyPermission: (permissions: Permission[]) => boolean;
  hasAllPermissions: (permissions: Permission[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");

  useEffect(() => {
    let active = true;
    const session = getAuthSession();
    if (!session) {
      setStatus("unauthenticated");
      return () => { active = false; };
    }
    authApi.me().then((nextUser) => {
      if (!active) return;
      if (!isInternalUser(nextUser)) {
        clearAuthSession();
        setStatus("unauthenticated");
        return;
      }
      setUser(nextUser);
      setStatus("authenticated");
    }).catch(() => {
      if (!active) return;
      clearAuthSession();
      setUser(null);
      setStatus("unauthenticated");
    });
    return () => { active = false; };
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    status,
    login: async (email, password) => {
      const tokens = await authApi.login({ email, password });
      saveAuthSession(tokens);
      try {
        const nextUser = await authApi.me();
        if (!isInternalUser(nextUser)) {
          clearAuthSession();
          throw new Error("This application is restricted to internal users.");
        }
        setUser(nextUser);
        setStatus("authenticated");
        return nextUser;
      } catch (error) {
        clearAuthSession();
        setUser(null);
        setStatus("unauthenticated");
        throw error;
      }
    },
    logout: async () => {
      const refreshToken = getAuthSession()?.refreshToken;
      try {
        if (refreshToken) await authApi.logout(refreshToken);
      } finally {
        clearAuthSession();
        setUser(null);
        setStatus("unauthenticated");
      }
    },
    hasPermission: (permission) => hasPermission(user, permission),
    hasAnyPermission: (permissions) => hasAnyPermission(user, permissions),
    hasAllPermissions: (permissions) => hasAllPermissions(user, permissions),
  }), [status, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
