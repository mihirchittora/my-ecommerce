import { RequireAuth } from "@/components/require-auth";
import { OrderDetail } from "@/components/order-detail";

export default async function OrderPage({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ new?: string }> }) { const { id } = await params; const query = await searchParams; return <div className="page-shell py-10 md:py-16"><RequireAuth><OrderDetail id={decodeURIComponent(id)} isNew={query.new === "1"} /></RequireAuth></div>; }
