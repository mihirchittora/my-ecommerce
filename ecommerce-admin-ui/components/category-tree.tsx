"use client";

import { ChevronDown, ChevronRight, Edit3, MoreHorizontal, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import type { CategoryNode } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { CategoryImage } from "@/components/category-image";

export function CategoryTree({ nodes, onEdit, onAddChild, onDelete }: { nodes: CategoryNode[]; onEdit: (node: CategoryNode) => void; onAddChild: (node: CategoryNode) => void; onDelete: (node: CategoryNode) => void }) {
  return <div className="divide-y divide-slate-100">{nodes.map((node) => <TreeNode key={node.id} node={node} depth={0} onEdit={onEdit} onAddChild={onAddChild} onDelete={onDelete} />)}</div>;
}

function TreeNode({ node, depth, onEdit, onAddChild, onDelete }: { node: CategoryNode; depth: number; onEdit: (node: CategoryNode) => void; onAddChild: (node: CategoryNode) => void; onDelete: (node: CategoryNode) => void }) {
  const [expanded, setExpanded] = useState(true);
  const hasChildren = node.children.length > 0;
  return <div><div className="group flex items-center gap-2 px-4 py-3.5 hover:bg-slate-50" style={{ paddingLeft: `${16 + depth * 28}px` }}>{hasChildren ? <button aria-label={`${expanded ? "Collapse" : "Expand"} ${node.name}`} className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-200" onClick={() => setExpanded((value) => !value)}>{expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}</button> : <span className="w-7" />}<div className="relative h-11 w-11 shrink-0 overflow-hidden rounded-xl bg-slate-100"><CategoryImage category={node} sizes="44px" /></div><div className="min-w-0 flex-1"><p className="truncate text-sm font-semibold text-slate-800">{node.name}</p><p className="truncate text-xs text-slate-400">/{node.slug}</p></div><Badge variant={node.status === "ACTIVE" ? "success" : "muted"}>{node.status}</Badge><DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" size="icon-sm" className="opacity-60 group-hover:opacity-100" aria-label={`Actions for ${node.name}`}><MoreHorizontal className="h-4 w-4" /></Button></DropdownMenuTrigger><DropdownMenuContent align="end"><DropdownMenuItem onSelect={() => onAddChild(node)}><Plus className="mr-2 h-4 w-4" />Add child</DropdownMenuItem><DropdownMenuItem onSelect={() => onEdit(node)}><Edit3 className="mr-2 h-4 w-4" />Edit category</DropdownMenuItem><DropdownMenuItem className="text-rose-600 focus:text-rose-600" onClick={() => onDelete(node)}><Trash2 className="mr-2 h-4 w-4" />Delete</DropdownMenuItem></DropdownMenuContent></DropdownMenu></div>{expanded && hasChildren && <div>{node.children.map((child) => <TreeNode key={child.id} node={child} depth={depth + 1} onEdit={onEdit} onAddChild={onAddChild} onDelete={onDelete} />)}</div>}</div>;
}
