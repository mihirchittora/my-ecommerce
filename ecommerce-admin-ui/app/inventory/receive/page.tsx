"use client";

import Link from "next/link";
import { ArrowLeft, CheckCircle2 } from "lucide-react";
import { useState } from "react";
import { PageIntro } from "@/components/page-intro";
import { ReceiveInventoryForm } from "@/components/inventory/forms";
import { LoadingCard } from "@/components/feedback-states";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useInventoryLocations, useReceiveInventory } from "@/lib/queries";
import { useToast } from "@/components/ui/toast";
import type { InventoryUnit } from "@/lib/types";

export default function ReceiveInventoryPage() { const locations = useInventoryLocations(); const receive = useReceiveInventory(); const [result, setResult] = useState<InventoryUnit[] | null>(null); const { toast } = useToast(); if (locations.isLoading) return <LoadingCard rows={7} />; return <><div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/inventory"><ArrowLeft className="h-4 w-4" />Back to inventory</Link></Button></div><PageIntro eyebrow="Inventory operations" title="Receive inventory" description="Create one persistent physical unit per quantity. The backend assigns the Inventory Unit IDs." />{result ? <Card className="mb-6 border-emerald-200 bg-emerald-50/50"><CardHeader><CardTitle className="flex items-center gap-2 text-emerald-800"><CheckCircle2 className="h-5 w-5" />Inventory received</CardTitle></CardHeader><CardContent><p className="text-sm text-emerald-700">{result.length} unit{result.length === 1 ? "" : "s"} created successfully.</p><div className="mt-4 flex flex-wrap gap-2">{result.map((unit) => <Link href={`/inventory/units/${unit.id}`} key={unit.id} className="rounded-lg bg-white px-2.5 py-1.5 font-mono text-xs font-semibold text-primary shadow-sm">{unit.id}</Link>)}</div><Button variant="outline" className="mt-5" onClick={() => setResult(null)}>Receive more</Button></CardContent></Card> : null}<ReceiveInventoryForm locations={locations.data ?? []} onSubmit={(payload) => receive.mutate(payload, { onSuccess: (units) => { setResult(units); toast({ title: "Inventory received", description: `${units.length} physical unit${units.length === 1 ? "" : "s"} created.` }); } })} isSubmitting={receive.isPending} serverError={receive.error} /></>;
}
