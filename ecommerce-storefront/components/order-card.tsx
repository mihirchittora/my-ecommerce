"use client";

import Link from "next/link";
import { ArrowRight, Package } from "lucide-react";
import type { OrderSummary } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { formatCurrency, formatDate } from "@/lib/utils";

export function OrderCard({ order }: { order: OrderSummary }) {
  return <Card className="p-5"><div className="flex flex-wrap items-start justify-between gap-4"><div><div className="flex items-center gap-3"><Package className="h-5 w-5 text-moss" /><h2 className="font-semibold">{order.orderNumber}</h2></div><p className="mt-2 text-sm text-ink/55">Placed {formatDate(order.createdAt)}</p></div><Badge tone={order.status === "CANCELLED" || order.status === "FAILED" ? "danger" : order.status === "PAID" || order.status === "CONFIRMED" || order.status === "DELIVERED" || order.status === "COMPLETED" ? "success" : "warning"}>{order.status.replaceAll("_", " ")}</Badge></div><div className="mt-5 flex items-end justify-between gap-4 border-t border-ink/10 pt-4"><p className="font-display text-xl font-bold">{formatCurrency(order.totalAmount, order.currency)}</p><Link href={`/orders/${encodeURIComponent(order.id)}`} className="inline-flex items-center gap-2 text-sm font-bold text-moss hover:text-ink">View order <ArrowRight className="h-4 w-4" /></Link></div></Card>;
}
