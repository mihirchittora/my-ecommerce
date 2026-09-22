import { Suspense } from "react";
import { ProductsBrowser } from "@/components/products-browser";
import { LoadingBlock } from "@/components/feedback";

export const metadata = { title: "Search" };
export default function SearchPage() { return <Suspense fallback={<LoadingBlock label="Searching products" />}><ProductsBrowser searchOnly /></Suspense>; }
