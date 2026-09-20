import { Badge } from "@/components/ui/badge";
import type { InventoryUnitStatus, LocationStatus, ReservationStatus } from "@/lib/types";
import { titleCase } from "@/lib/utils";

const unitVariants: Record<InventoryUnitStatus, "success" | "warning" | "muted" | "danger" | "default"> = { AVAILABLE: "success", RESERVED: "warning", ALLOCATED: "default", IN_TRANSIT: "default", SOLD: "muted", RETURNED: "warning", DAMAGED: "danger", LOST: "danger" };
const reservationVariants: Record<ReservationStatus, "success" | "warning" | "muted" | "danger" | "default"> = { ACTIVE: "success", CONFIRMED: "default", RELEASED: "muted", EXPIRED: "warning", CANCELLED: "danger" };

export function UnitStatusBadge({ status }: { status: InventoryUnitStatus }) { return <Badge variant={unitVariants[status] ?? "muted"}>{titleCase(status)}</Badge>; }
export function ReservationStatusBadge({ status }: { status: ReservationStatus }) { return <Badge variant={reservationVariants[status] ?? "muted"}>{titleCase(status)}</Badge>; }
export function LocationStatusBadge({ status }: { status: LocationStatus }) { return <Badge variant={status === "ACTIVE" ? "success" : "muted"}>{titleCase(status)}</Badge>; }
