import { ClipboardList } from "lucide-react";
import { EmptyState } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";

export default function OrdersPage() { return <><PageIntro eyebrow="Future module" title="Orders" description="Order operations will appear here once the Orders service is available." /><EmptyState title="Orders API not connected" message="This navigation entry is intentionally reserved for the Orders service. No production data is fabricated here." action={<div className="flex items-center gap-2 text-sm text-slate-400"><ClipboardList className="h-4 w-4" />Coming soon</div>} /></>; }
