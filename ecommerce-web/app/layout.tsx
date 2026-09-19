import type { Metadata } from "next";
import { Providers } from "@/components/providers";
import { AppShell } from "@/components/app-shell";
import "@/app/globals.css";

export const metadata: Metadata = { title: "Meridian Catalog", description: "Product catalog management workspace" };

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body><Providers><AppShell>{children}</AppShell></Providers></body></html>;
}
