import { authClient } from "@/lib/api/client";
import type { AuthTokenResponse, AuthUser } from "@/lib/types";

export const authApi = {
  login: (payload: { email: string; password: string }) =>
    authClient.json<AuthTokenResponse>("/v1/auth/login", payload),
  refresh: (refreshToken: string) =>
    authClient.json<AuthTokenResponse>("/v1/auth/refresh", { refreshToken }),
  me: () => authClient.request<AuthUser>("/v1/auth/me"),
  logout: (refreshToken: string) =>
    authClient.json<void>("/v1/auth/logout", { refreshToken }, { method: "POST" }),
};
