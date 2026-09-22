import type { Metadata } from "next";
import { Providers } from "@/components/providers";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";
import "@/app/globals.css";

export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3001"),
  title: { default: "Morrow — thoughtful everyday goods", template: "%s | Morrow" },
  description: "A considered collection for everyday living.",
  openGraph: { type: "website", siteName: "Morrow" },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body><Providers><SiteHeader /><main className="min-h-[60vh]">{children}</main><SiteFooter /></Providers></body></html>;
}
