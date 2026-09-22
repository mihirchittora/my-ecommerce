import TrackingPage from "@/components/tracking-page";
export default async function TrackingRoute({ params }: { params: Promise<{ id: string }> }) { const { id } = await params; return <TrackingPage orderId={decodeURIComponent(id)} />; }
