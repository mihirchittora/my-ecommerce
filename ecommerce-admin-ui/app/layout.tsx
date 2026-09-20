import type { Metadata } from "next";
import { Providers } from "@/components/providers";
import { AuthGate } from "@/components/auth-gate";
import "@/app/globals.css";

export const metadata: Metadata = { title: "Meridian Admin", description: "Internal catalog and inventory operations workspace" };

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body><Providers><AuthGate>{children}</AuthGate></Providers></body></html>;
}
