"use client";

import * as DropdownMenuPrimitive from "@radix-ui/react-dropdown-menu";
import * as React from "react";
import { cn } from "@/lib/utils";

export const DropdownMenu = DropdownMenuPrimitive.Root;
export const DropdownMenuTrigger = DropdownMenuPrimitive.Trigger;
export const DropdownMenuGroup = DropdownMenuPrimitive.Group;
export const DropdownMenuPortal = DropdownMenuPrimitive.Portal;
export const DropdownMenuSub = DropdownMenuPrimitive.Sub;
export const DropdownMenuRadioGroup = DropdownMenuPrimitive.RadioGroup;

export const DropdownMenuContent = React.forwardRef<React.ElementRef<typeof DropdownMenuPrimitive.Content>, React.ComponentPropsWithoutRef<typeof DropdownMenuPrimitive.Content>>(({ className, sideOffset = 6, ...props }, ref) => <DropdownMenuPrimitive.Portal><DropdownMenuPrimitive.Content ref={ref} sideOffset={sideOffset} className={cn("z-50 min-w-40 overflow-hidden rounded-xl border bg-popover p-1 text-popover-foreground shadow-soft", className)} {...props} /></DropdownMenuPrimitive.Portal>);
DropdownMenuContent.displayName = DropdownMenuPrimitive.Content.displayName;
export const DropdownMenuItem = React.forwardRef<React.ElementRef<typeof DropdownMenuPrimitive.Item>, React.ComponentPropsWithoutRef<typeof DropdownMenuPrimitive.Item>>(({ className, ...props }, ref) => <DropdownMenuPrimitive.Item ref={ref} className={cn("relative flex cursor-default select-none items-center rounded-lg px-2.5 py-2 text-sm outline-none transition-colors focus:bg-accent focus:text-accent-foreground", className)} {...props} />);
DropdownMenuItem.displayName = DropdownMenuPrimitive.Item.displayName;
