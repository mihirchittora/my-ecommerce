import type { CategoryNode } from "@/lib/types";

export function flattenCategoryOptions(nodes: CategoryNode[], depth = 0): Array<{ id: string; name: string; depth: number }> { return nodes.flatMap((node) => [{ id: node.id, name: node.name, depth }, ...flattenCategoryOptions(node.children, depth + 1)]); }
