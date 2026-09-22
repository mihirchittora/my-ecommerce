import type { NextConfig } from "next";

const serviceUrls = {
  auth: process.env.NEXT_PUBLIC_AUTH_API_URL ?? "http://localhost:8085",
  catalog: process.env.NEXT_PUBLIC_CATALOG_API_URL ?? "http://localhost:8081",
  inventory: process.env.NEXT_PUBLIC_INVENTORY_API_URL ?? "http://localhost:8082",
  order: process.env.NEXT_PUBLIC_ORDER_API_URL ?? "http://localhost:8083",
  cart: process.env.NEXT_PUBLIC_CART_API_URL ?? "http://localhost:8084",
  customer: process.env.NEXT_PUBLIC_CUSTOMER_API_URL ?? "http://localhost:8086",
  payment: process.env.NEXT_PUBLIC_PAYMENT_API_URL ?? "http://localhost:8087",
  shipping: process.env.NEXT_PUBLIC_SHIPPING_API_URL ?? "http://localhost:8088",
} as const;

const nextConfig: NextConfig = {
  reactStrictMode: true,
  outputFileTracingRoot: process.cwd(),
  async rewrites() {
    return Object.entries(serviceUrls).map(([service, origin]) => ({
      source: `/backend/${service}/:path*`,
      destination: `${origin}/api/:path*`,
    }));
  },
};

export default nextConfig;
