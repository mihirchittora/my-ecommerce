import { Badge } from "@/components/ui/badge";
import { titleCase } from "@/lib/utils";

export function StatusBadge({ status }: { status: string }) { const variant = status === "ACTIVE" ? "success" : status === "DRAFT" ? "warning" : "muted"; return <Badge variant={variant}>{titleCase(status)}</Badge>; }
