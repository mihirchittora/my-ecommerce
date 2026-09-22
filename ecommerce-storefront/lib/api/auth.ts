import { authClient } from "@/lib/api/client";
import type { AuthTokenResponse, AuthUser } from "@/lib/types";

export const authApi = {
  login: (payload: { email: string; password: string }) => authClient.json<AuthTokenResponse, typeof payload>("/v1/auth/login", payload),
  register: (payload: { email: string; password: string; firstName: string; lastName: string }) => authClient.json<AuthUser, typeof payload>("/v1/auth/register", payload),
  refresh: (refreshToken: string) => authClient.json<AuthTokenResponse, { refreshToken: string }>("/v1/auth/refresh", { refreshToken }),
  me: () => authClient.request<AuthUser>("/v1/auth/me"),
  logout: (refreshToken: string) => authClient.json<void, { refreshToken: string }>("/v1/auth/logout", { refreshToken }),
};
