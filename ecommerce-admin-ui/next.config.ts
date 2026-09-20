import type { NextConfig } from "next";

const authApiUrl = process.env.NEXT_PUBLIC_AUTH_API_URL ?? "http://localhost:8085";
const catalogApiUrl = process.env.NEXT_PUBLIC_CATALOG_API_URL ?? "http://localhost:8081";
const inventoryApiUrl = process.env.NEXT_PUBLIC_INVENTORY_API_URL ?? "http://localhost:8082";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  async rewrites() {
    return [
      {
        source: "/backend/auth/:path*",
        destination: `${authApiUrl}/api/:path*`,
      },
      {
        source: "/backend/catalog/:path*",
        destination: `${catalogApiUrl}/api/:path*`,
      },
      {
        source: "/backend/inventory/:path*",
        destination: `${inventoryApiUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
