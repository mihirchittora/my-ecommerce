import type { NextConfig } from "next";

const authApiUrl = process.env.NEXT_PUBLIC_AUTH_API_URL ?? "http://localhost:8085";
const catalogApiUrl = process.env.NEXT_PUBLIC_CATALOG_API_URL ?? "http://localhost:8081";
const inventoryApiUrl = process.env.NEXT_PUBLIC_INVENTORY_API_URL ?? "http://localhost:8082";
const orderApiUrl = process.env.NEXT_PUBLIC_ORDER_API_URL ?? "http://localhost:8083";
const cartApiUrl = process.env.NEXT_PUBLIC_CART_API_URL ?? "http://localhost:8084";
const customerApiUrl = process.env.NEXT_PUBLIC_CUSTOMER_API_URL ?? "http://localhost:8086";

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
      {
        source: "/backend/order/:path*",
        destination: `${orderApiUrl}/api/:path*`,
      },
      {
        source: "/backend/cart/:path*",
        destination: `${cartApiUrl}/api/:path*`,
      },
      {
        source: "/backend/customer/:path*",
        destination: `${customerApiUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
