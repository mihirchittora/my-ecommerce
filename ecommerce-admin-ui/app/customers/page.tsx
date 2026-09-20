import { Users } from "lucide-react";
import { EmptyState } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";

export default function CustomersPage() { return <><PageIntro eyebrow="Future module" title="Customers" description="Customer operations will appear here once the Customers service is available." /><EmptyState title="Customers API not connected" message="This navigation entry is intentionally reserved for the Customers service. No production data is fabricated here." action={<div className="flex items-center gap-2 text-sm text-slate-400"><Users className="h-4 w-4" />Coming soon</div>} /></>; }
