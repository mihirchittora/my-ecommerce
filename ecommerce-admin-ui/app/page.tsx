"use client";

import Link from "next/link";
import { ArrowRight, Boxes, ClipboardList, Cuboid, Layers3, Package, Plus, Shapes, Sparkles } from "lucide-react";
import { useAuth } from "@/components/auth-provider";
import { CheckoutDashboardSection } from "@/components/dashboard/checkout-dashboard-section";
import { AccessDashboardSection } from "@/components/dashboard/access-dashboard-section";
import { CustomerDashboardSection } from "@/components/dashboard/customer-dashboard-section";
import { OrderDashboardSection } from "@/components/dashboard/order-dashboard-section";
import { ErrorState } from "@/components/feedback-states";
import { PageIntro } from "@/components/page-intro";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { hasPermission } from "@/lib/permissions";
import { useCategoryTree, useInventoryDashboardSummary, useProductList } from "@/lib/queries";

function countCategories(nodes: Array<{ children: unknown[] }>): number {
  return nodes.reduce((count, node) => count + 1 + countCategories(node.children as Array<{ children: unknown[] }>), 0);
}

export default function DashboardPage() {
  const { user } = useAuth();
  const canReadCategories = hasPermission(user, "CATEGORY_READ");
  const canReadProducts = hasPermission(user, "PRODUCT_READ");
  const canReadInventory = hasPermission(user, "INVENTORY_READ");
  const canReadPayments = hasPermission(user, "PAYMENT_READ");
  const canCreateProducts = hasPermission(user, "PRODUCT_CREATE");

  const categories = useCategoryTree(canReadCategories);
  const products = useProductList({ page: 0, size: 1, sort: "createdAt,desc" }, canReadProducts);
  const activeProducts = useProductList({ page: 0, size: 1, sort: "createdAt,desc", status: "ACTIVE" }, canReadProducts);
  const inventory = useInventoryDashboardSummary(canReadInventory);
  const categoryCount = categories.data ? countCategories(categories.data) : 0;
  const hasDashboardErrors =
    (canReadCategories && categories.isError) ||
    (canReadProducts && (products.isError || activeProducts.isError)) ||
    (canReadInventory && inventory.isError);

  return (
    <div className="page-grid -m-4 min-h-[calc(100vh-5rem)] p-4 sm:-m-6 sm:p-6 lg:-m-10 lg:p-10">
      <PageIntro
        eyebrow="Operations overview"
        title="Good morning, operations team"
        description="A focused view of catalog, inventory, orders and checkout operations."
        action={canCreateProducts ? { label: "Add product", href: "/products/new" } : undefined}
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Products" value={canReadProducts ? products.data?.totalElements ?? "—" : "—"} icon={Package} tone="blue" href={canReadProducts ? "/products" : undefined} />
        <MetricCard label="Categories" value={canReadCategories ? (categories.isLoading ? "—" : categoryCount) : "—"} icon={Shapes} tone="violet" href={canReadCategories ? "/categories" : undefined} />
        <MetricCard label="Active products" value={canReadProducts ? activeProducts.data?.totalElements ?? "—" : "—"} icon={Layers3} tone="amber" />
        <MetricCard label="Catalog status" value={!canReadProducts && !canReadCategories ? "—" : products.isError || activeProducts.isError ? "Unavailable" : "Healthy"} icon={Sparkles} tone="green" />
      </div>

      {canReadInventory && (
        <section className="mt-6">
          <div className="mb-4 flex items-end justify-between">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">Inventory service</p>
              <h2 className="mt-1 text-lg font-semibold text-slate-900">Stock health</h2>
            </div>
            <Link href="/inventory" className="text-sm font-semibold text-primary hover:underline">Open inventory</Link>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-6">
            <MetricCard label="Total units" value={inventory.data?.totalInventoryUnits ?? "—"} icon={Cuboid} tone="blue" href="/inventory/units" />
            <MetricCard label="Available" value={inventory.data?.availableUnits ?? "—"} icon={Cuboid} tone="green" />
            <MetricCard label="Reserved" value={inventory.data?.reservedUnits ?? "—"} icon={ClipboardList} tone="violet" />
            <MetricCard label="Damaged" value={inventory.data?.damagedUnits ?? "—"} icon={Cuboid} tone="amber" />
            <MetricCard label="Active locations" value={inventory.data?.activeLocations ?? "—"} icon={Package} tone="blue" href="/inventory/locations" />
            <MetricCard label="Inventory status" value={inventory.isError ? "Unavailable" : "Healthy"} icon={Sparkles} tone="green" />
          </div>
          <p className="mt-3 text-sm text-slate-500">Totals are calculated from itemized InventoryUnit records. Open a SKU or location for detailed stock and unit information.</p>
        </section>
      )}

      <OrderDashboardSection />
      <CheckoutDashboardSection />
      <AccessDashboardSection />
      <CustomerDashboardSection />

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.4fr_0.8fr]">
        <Card>
          <CardHeader className="flex-row items-center justify-between">
            <div>
              <CardTitle>Quick actions</CardTitle>
              <p className="mt-1 text-sm text-muted-foreground">Common catalog and inventory operations, one click away.</p>
            </div>
            <Boxes className="h-5 w-5 text-slate-300" />
          </CardHeader>
          <CardContent className="grid gap-3 sm:grid-cols-2">
            {canCreateProducts && <QuickAction href="/products/new" icon={Plus} title="Create a product" description="Add product details, variants and pricing." />}
            {canReadCategories && <QuickAction href="/categories" icon={Shapes} title="Organize categories" description="Build a clear hierarchy for browsing." />}
            {hasPermission(user, "INVENTORY_RECEIVE") && <QuickAction href="/inventory/receive" icon={Cuboid} title="Receive inventory" description="Add physical units to a location." />}
            {canReadInventory && <QuickAction href="/inventory/reservations" icon={ClipboardList} title="Review reservations" description="Create, confirm or release reserved units." />}
            {canReadPayments && <QuickAction href="/payments" icon={ClipboardList} title="Review payments" description="Inspect provider attempts and refund state." />}
            {!canCreateProducts && !canReadCategories && !hasPermission(user, "INVENTORY_RECEIVE") && !canReadInventory && !canReadPayments && <p className="text-sm text-slate-500">Use the commerce panels above to monitor order and checkout activity.</p>}
          </CardContent>
        </Card>

        <Card className="overflow-hidden bg-slate-950 text-white">
          <CardHeader>
            <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-white/10"><Sparkles className="h-5 w-5 text-blue-300" /></div>
            <CardTitle className="text-white">Operating model</CardTitle>
            <p className="mt-1 text-sm leading-6 text-slate-400">SKU means a sellable configuration. Inventory Unit ID means one physical item. Serial number and IMEI are optional identifiers.</p>
          </CardHeader>
          <CardContent>
            {canReadInventory ? <Link href="/inventory/units" className="inline-flex items-center gap-2 text-sm font-semibold text-blue-300 hover:text-blue-200">View itemized units <ArrowRight className="h-4 w-4" /></Link> : <p className="text-sm text-slate-400">Order totals and checkout lifecycle data are sourced from their owning services.</p>}
          </CardContent>
        </Card>
      </div>

      {hasDashboardErrors && <div className="mt-6"><ErrorState message="One or more dashboard services are unavailable. Open the corresponding Catalog or Inventory screen for details." /></div>}
    </div>
  );
}

function MetricCard({ label, value, icon: Icon, tone, href }: { label: string; value: string | number; icon: typeof Package; tone: "blue" | "violet" | "amber" | "green"; href?: string }) {
  const styles = { blue: "bg-blue-50 text-blue-600", violet: "bg-violet-50 text-violet-600", amber: "bg-amber-50 text-amber-600", green: "bg-emerald-50 text-emerald-600" };
  const body = <Card className="p-5"><div className="flex items-start justify-between"><div><p className="text-sm font-medium text-slate-500">{label}</p><p className="mt-3 text-2xl font-bold tracking-tight text-slate-950">{value}</p></div><div className={`flex h-10 w-10 items-center justify-center rounded-xl ${styles[tone]}`}><Icon className="h-5 w-5" /></div></div></Card>;
  return href ? <Link href={href} className="block transition-transform hover:-translate-y-0.5">{body}</Link> : body;
}

function QuickAction({ href, icon: Icon, title, description }: { href: string; icon: typeof Plus; title: string; description: string }) {
  return <Link href={href} className="group rounded-2xl border border-slate-100 p-4 transition-colors hover:border-blue-100 hover:bg-blue-50/50"><div className="flex items-center gap-3"><div className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-slate-500 group-hover:bg-blue-100 group-hover:text-primary"><Icon className="h-4 w-4" /></div><div className="min-w-0"><p className="text-sm font-semibold text-slate-800">{title}</p><p className="mt-1 text-xs leading-5 text-slate-500">{description}</p></div><ArrowRight className="ml-auto h-4 w-4 text-slate-300 group-hover:text-primary" /></div></Link>;
}
