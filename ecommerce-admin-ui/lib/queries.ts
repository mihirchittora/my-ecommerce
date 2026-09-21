"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { categoryApi, fetchCategoryTree } from "@/lib/api/categories";
import { imageApi } from "@/lib/api/images";
import { productApi } from "@/lib/api/products";
import { inventoryApi } from "@/lib/api/inventory";
import type { CategoryPayload, InventoryAdjustmentPayload, InventoryReceivePayload, InventoryReservationPayload, InventoryTransferPayload, InventoryUnit, PageResponse, ProductListParams, ProductPayload } from "@/lib/types";

export const queryKeys = {
  categories: ["categories"] as const,
  categoryTree: ["categories", "tree"] as const,
  products: ["products"] as const,
  product: (id: string) => ["products", id] as const,
  inventorySummary: ["inventory", "summary"] as const,
  inventoryDashboardSummary: ["inventory", "dashboard-summary"] as const,
  inventoryStock: (params: unknown) => ["inventory", "stock", params] as const,
  inventoryLocations: ["inventory", "locations"] as const,
  inventoryUnits: (params: unknown) => ["inventory", "units", params] as const,
  inventoryUnit: (id: string) => ["inventory", "unit", id] as const,
  reservations: (params: unknown) => ["reservations", params] as const,
  reservation: (id: string) => ["reservation", id] as const,
  adjustments: (params: unknown) => ["adjustments", params] as const,
  transfers: (params: unknown) => ["transfers", params] as const,
  reconciliation: ["inventory", "reconciliation"] as const,
};

export function useCategoryTree(enabled = true) {
  return useQuery({ queryKey: queryKeys.categoryTree, queryFn: fetchCategoryTree, enabled });
}

export function useProductList(params: ProductListParams, enabled = true) {
  return useQuery({ queryKey: [...queryKeys.products, params], queryFn: () => productApi.list(params), enabled, placeholderData: (previous) => previous });
}

export function useProduct(id: string) {
  return useQuery({ queryKey: queryKeys.product(id), queryFn: () => productApi.get(id), enabled: Boolean(id) });
}

export function useCreateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CategoryPayload) => categoryApi.create(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.categoryTree }),
  });
}

export function useUpdateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: CategoryPayload }) => categoryApi.update(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.categoryTree }),
  });
}

export function useDeleteCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => categoryApi.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.categoryTree }),
  });
}

export function useCreateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ProductPayload) => productApi.create(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.products }),
  });
}

export function useUpdateProduct(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ProductPayload) => productApi.update(id, payload),
    onSuccess: (product) => {
      queryClient.setQueryData(queryKeys.product(id), product);
      queryClient.invalidateQueries({ queryKey: queryKeys.products });
    },
  });
}

export function useDeleteProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => productApi.delete(id),
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: queryKeys.product(id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.products });
    },
  });
}

export function useInventorySummary(sku: string) {
  return useQuery({ queryKey: [...queryKeys.inventorySummary, sku], queryFn: () => inventoryApi.summary(sku), enabled: Boolean(sku) });
}

export function useInventoryDashboardSummary(enabled = true) {
  return useQuery({ queryKey: queryKeys.inventoryDashboardSummary, queryFn: inventoryApi.dashboardSummary, enabled });
}

export function useInventoryStock(params: Parameters<typeof inventoryApi.stock>[0]) {
  return useQuery({ queryKey: queryKeys.inventoryStock(params), queryFn: () => inventoryApi.stock(params), placeholderData: (previous) => previous });
}

export function useInventoryLocations() {
  return useQuery({ queryKey: queryKeys.inventoryLocations, queryFn: inventoryApi.locations });
}

export function useInventoryUnits(params: Parameters<typeof inventoryApi.units>[0]) {
  return useQuery<PageResponse<InventoryUnit>>({ queryKey: queryKeys.inventoryUnits(params), queryFn: () => inventoryApi.units(params), enabled: Boolean(params.sku), placeholderData: (previous: PageResponse<InventoryUnit> | undefined) => previous });
}

export function useInventoryUnit(id: string) {
  return useQuery({ queryKey: queryKeys.inventoryUnit(id), queryFn: () => inventoryApi.unit(id), enabled: Boolean(id) });
}

export function useInventoryReservations(params: Parameters<typeof inventoryApi.reservations>[0]) {
  return useQuery({ queryKey: queryKeys.reservations(params), queryFn: () => inventoryApi.reservations(params), placeholderData: (previous) => previous });
}

export function useReservation(id: string) {
  return useQuery({ queryKey: queryKeys.reservation(id), queryFn: () => inventoryApi.reservation(id), enabled: Boolean(id) });
}

export function useInventoryAdjustments(params: Parameters<typeof inventoryApi.adjustments>[0]) {
  return useQuery({ queryKey: queryKeys.adjustments(params), queryFn: () => inventoryApi.adjustments(params), placeholderData: (previous) => previous });
}

export function useInventoryTransfers(params: Parameters<typeof inventoryApi.transfers>[0]) {
  return useQuery({ queryKey: queryKeys.transfers(params), queryFn: () => inventoryApi.transfers(params), placeholderData: (previous) => previous });
}

export function useReconciliation(sku?: string, locationId?: string) {
  return useQuery({ queryKey: [...queryKeys.reconciliation, sku, locationId], queryFn: () => inventoryApi.reconciliation(sku as string, locationId as string), enabled: Boolean(sku && locationId) });
}

export function useCreateLocation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: inventoryApi.createLocation, onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.inventoryLocations }) });
}

export function useUpdateLocation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ id, payload }: { id: string; payload: Parameters<typeof inventoryApi.updateLocation>[1] }) => inventoryApi.updateLocation(id, payload), onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.inventoryLocations }) });
}

export function useDeleteLocation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: inventoryApi.deleteLocation, onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.inventoryLocations }) });
}

export function useReceiveInventory() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: (payload: InventoryReceivePayload) => inventoryApi.receive(payload), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["inventory"] }); } });
}

export function useCreateReservation() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: (payload: InventoryReservationPayload) => inventoryApi.createReservation(payload), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["reservations"] }) });
}

export function useReservationAction() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: ({ id, action }: { id: string; action: "confirm" | "release" | "cancel" }) => inventoryApi.reservationAction(id, action), onSuccess: (reservation) => { queryClient.setQueryData(queryKeys.reservation(reservation.id), reservation); queryClient.invalidateQueries({ queryKey: ["reservations"] }); queryClient.invalidateQueries({ queryKey: ["inventory"] }); } });
}

export function useCreateAdjustment() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: (payload: InventoryAdjustmentPayload) => inventoryApi.createAdjustment(payload), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["adjustments"] }); queryClient.invalidateQueries({ queryKey: ["inventory"] }); } });
}

export function useCreateTransfer() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: (payload: InventoryTransferPayload) => inventoryApi.createTransfer(payload), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["transfers"] }); queryClient.invalidateQueries({ queryKey: ["inventory"] }); } });
}

export function useUploadImage(productId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ file, sortOrder, variantId }: { file: File; sortOrder: number; variantId?: string }) => imageApi.upload(productId, file, sortOrder, variantId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.product(productId) }),
  });
}

export function useDeleteImage(productId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (imageId: string) => imageApi.delete(productId, imageId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.product(productId) }),
  });
}
