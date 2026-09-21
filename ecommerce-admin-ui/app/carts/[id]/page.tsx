"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useParams } from "next/navigation";
import { useMemo } from "react";
import { CartItems, CartInventoryPanel, CartLifecycleNote, CartOverview, CartWarnings, CustomerAccessNote } from "@/components/carts/cart-detail-sections";
import { CartStatusBadge } from "@/components/carts/cart-status-badge";
import { ErrorState, LoadingCard } from "@/components/feedback-states";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/client";
import { useCart, useCartInventory } from "@/lib/api/cart/queries";
import { useOrder } from "@/lib/api/order/queries";
import { useAuth } from "@/components/auth-provider";
import { formatDate } from "@/lib/utils";

function errorState(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 403) return { title: "Cart access denied", message: "You do not have permission to view customer carts." };
    if (error.status === 404) return { title: "Cart not found", message: "The cart may have been removed or the link may be invalid." };
    if (error.status === 0 || error.status >= 500) return { title: "Cart service unavailable", message: "Cart data is temporarily unavailable. Catalog and Inventory data are not required to load the Cart itself." };
    return { title: "Unable to load cart", message: error.message };
  }
  return { title: "Unable to load cart", message: "Cart service is temporarily unavailable." };
}

export default function CartDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { hasPermission } = useAuth();
  const cart = useCart(id);
  const canReadInventory = hasPermission("INVENTORY_READ");
  const canReadProducts = hasPermission("PRODUCT_READ");
  const canReadOrders = hasPermission("ORDER_READ");
  const convertedOrderId = cart.data?.convertedOrderId ?? "";
  const order = useOrder(canReadOrders ? convertedOrderId : "");
  const skus = useMemo(() => cart.data?.items.map((item) => item.sku) ?? [], [cart.data?.items]);
  const inventoryQueries = useCartInventory(id, skus, canReadInventory);
  const retryInventory = () => void Promise.all(inventoryQueries.map((entry) => entry.query.refetch()));

  if (cart.isLoading) return <LoadingCard rows={8} />;
  if (cart.isError || !cart.data) {
    const error = errorState(cart.error);
    return <ErrorState title={error.title} message={error.message} onRetry={() => void cart.refetch()} />;
  }

  return <><div className="mb-5"><Button asChild variant="ghost" size="sm"><Link href="/carts"><ArrowLeft className="h-4 w-4" />Back to carts</Link></Button></div><div className="mb-8 flex flex-col gap-4 md:flex-row md:items-end md:justify-between"><div><p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">Cart detail</p><div className="flex flex-wrap items-center gap-3"><h1 className="font-mono text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">{cart.data.id}</h1><CartStatusBadge status={cart.data.status} /></div><p className="mt-2 text-sm text-slate-500">{cart.data.itemCount} item{cart.data.itemCount === 1 ? "" : "s"} · Updated {formatDate(cart.data.updatedAt)}</p></div><p className="max-w-sm text-sm leading-6 text-slate-500">Inspection only. No Cart mutation, checkout, or inventory reservation actions are available here.</p></div><div className="space-y-6"><CustomerAccessNote /><CartOverview cart={cart.data} canReadOrders={canReadOrders} orderStatus={order.data?.status} orderStatusUnavailable={order.isError} /><CartWarnings cart={cart.data} /><CartItems cart={cart.data} canReadProducts={canReadProducts} canReadInventory={canReadInventory} /><CartInventoryPanel cart={cart.data} queries={inventoryQueries} canReadInventory={canReadInventory} onRetry={retryInventory} /><CartLifecycleNote status={cart.data.status} /></div></>;
}
