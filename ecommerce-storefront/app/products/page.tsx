import { Suspense } from "react";
import { ProductsBrowser } from "@/components/products-browser";
import { LoadingBlock } from "@/components/feedback";

export const metadata = { title: "Shop all products" };
export default function ProductsPage() { return <Suspense fallback={<LoadingBlock label="Loading products" />}><ProductsBrowser /></Suspense>; }
