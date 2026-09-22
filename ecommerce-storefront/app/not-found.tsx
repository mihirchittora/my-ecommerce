import { Button } from "@/components/ui/button";

export default function NotFound() { return <div className="page-shell py-24 text-center"><p className="eyebrow">404</p><h1 className="mt-3 font-display text-4xl font-bold tracking-tight md:text-6xl">That page wandered off.</h1><p className="mx-auto mt-5 max-w-md text-ink/60">The product or page you requested is not available.</p><Button className="mt-8" asLink="/products">Browse the collection</Button></div>; }
