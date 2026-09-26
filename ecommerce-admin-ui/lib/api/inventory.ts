import { ApiError, inventoryClient } from "@/lib/api/client";
import type {
  InventoryAdjustment,
  InventoryAdjustmentPayload,
  InventoryItem,
  InventoryLocation,
  InventoryMovement,
  InventoryReceivePayload,
  InventoryReconciliationRow,
  InventoryReservation,
  InventoryReservationPayload,
  InventorySummary,
  ReservationStatus,
  InventoryStockRow,
  InventoryTransfer,
  InventoryTransferPayload,
  InventoryUnit,
  InventoryUnitStatus,
  LocationStatus,
  PageResponse,
} from "@/lib/types";

type ActualLocation = InventoryLocation;
type ActualItem = { id: string; sku: string; productName?: string | null; locationId: string; locationCode: string; locationName: string; quantity: number; reservedQuantity: number; available: number; version: number };
type ActualUnit = { id: string; unitCode: string; sku: string; productName?: string | null; locationId: string; locationCode: string; locationName: string; serialNumber: string | null; imei: string | null; barcode: string | null; status: InventoryUnitStatus; receivedAt: string; soldAt: string | null };
type ActualSummary = { sku: string; productName?: string | null; totalQuantity: number; totalReserved: number; totalAvailable: number; locations: Array<{ locationId: string; locationCode: string; locationName: string; quantity: number; reservedQuantity: number; available: number }> };
type ActualSummaryPage = PageResponse<ActualSummary>;
type ActualReceive = { inventory: ActualItem; units: ActualUnit[] };
type ActualAdjustment = { id: string; sku: string; locationId: string; locationCode: string; locationName: string; quantity: number; reason: InventoryAdjustment["reason"]; referenceId: string | null; createdAt: string };
type ActualReservation = { reservationId: string; sku: string; locationId: string; locationCode: string; quantity: number; referenceId: string; status: InventoryReservation["status"]; expiresAt: string | null; units: Array<{ unitId: string; unitCode: string; status: InventoryUnitStatus }>; createdAt: string; updatedAt: string };
type ActualReservationList = Omit<ActualReservation, "units">;
type ActualTransfer = { id: string; sku: string; fromLocation: { id: string; code: string; name: string; status?: LocationStatus } | null; toLocation: { id: string; code: string; name: string; status?: LocationStatus } | null; unitIds: string[]; referenceId: string | null; createdAt: string };

function query(values: Record<string, string | number | undefined | null>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => { if (value !== undefined && value !== null && value !== "") params.set(key, String(value)); });
  const text = params.toString();
  return text ? `?${text}` : "";
}

function unsupported<T>(message: string): Promise<T> { return Promise.reject(new ApiError(501, message, [], {}, "inventory")); }
function location(value: { id?: string; locationId?: string; code?: string; locationCode?: string; name?: string; locationName?: string }): InventoryLocation { return { id: value.id ?? value.locationId ?? "", code: value.code ?? value.locationCode ?? "", name: value.name ?? value.locationName ?? "", status: "ACTIVE" }; }
function unit(value: ActualUnit): InventoryUnit { return { id: value.id, unitCode: value.unitCode, sku: value.sku, productName: value.productName ?? null, location: location({ locationId: value.locationId, locationCode: value.locationCode, locationName: value.locationName }), locationId: value.locationId, status: value.status, serialNumber: value.serialNumber, imei: value.imei, barcode: value.barcode, receivedAt: value.receivedAt, soldAt: value.soldAt }; }
function item(value: ActualItem): InventoryItem { return { id: value.id, sku: value.sku, location: location({ locationId: value.locationId, locationCode: value.locationCode, locationName: value.locationName }), locationId: value.locationId, quantity: value.quantity, reservedQuantity: value.reservedQuantity, available: value.available, version: value.version }; }
function reservation(value: ActualReservation | ActualReservationList): InventoryReservation {
  const reservedUnits = "units" in value ? value.units : [];
  return { id: value.reservationId, sku: value.sku, location: location({ locationId: value.locationId, locationCode: value.locationCode }), quantity: value.quantity, referenceId: value.referenceId, status: value.status, expiresAt: value.expiresAt, units: reservedUnits.map((reserved) => ({ id: reserved.unitId, unitCode: reserved.unitCode, sku: value.sku, location: location({ locationId: value.locationId, locationCode: value.locationCode }), status: reserved.status, serialNumber: null, imei: null, barcode: null, receivedAt: value.createdAt, soldAt: null })), createdAt: value.createdAt };
}
function adjustment(value: ActualAdjustment): InventoryAdjustment { return { id: value.id, sku: value.sku, location: location({ locationId: value.locationId, locationCode: value.locationCode, locationName: value.locationName }), quantity: value.quantity, reason: value.reason, referenceId: value.referenceId, createdAt: value.createdAt }; }
function transfer(value: ActualTransfer): InventoryTransfer { return { id: value.id, sku: value.sku, fromLocation: value.fromLocation ? location(value.fromLocation) : location({}), toLocation: value.toLocation ? location(value.toLocation) : location({}), unitIds: value.unitIds, referenceId: value.referenceId, createdAt: value.createdAt }; }
function page<T>(content: T[], params: { page: number; size: number }): PageResponse<T> { return { content, totalElements: content.length, totalPages: content.length ? Math.ceil(content.length / params.size) : 0, number: params.page, size: params.size, numberOfElements: content.length, first: params.page === 0, last: true, empty: content.length === 0 }; }

export const inventoryApi = {
  summary: (sku: string) => inventoryClient.request<ActualSummary>(`/v1/inventory/${encodeURIComponent(sku)}`),
  dashboardSummary: () => inventoryClient.request<InventorySummary>("/v1/inventory/summary"),
  stock: async (params: { page: number; size: number; sort: string; sku?: string; locationId?: string }): Promise<PageResponse<InventoryStockRow>> => {
    if (!params.sku) {
      const response = await inventoryClient.request<ActualSummaryPage>(`/v1/inventory${query({ page: params.page, size: params.size, sort: params.sort })}`);
      return {
        ...response,
        content: response.content.map((summary) => ({
          sku: summary.sku,
          productName: summary.productName ?? null,
          location: summary.locations[0] ? location(summary.locations[0]) : location({}),
          locationCount: summary.locations.length,
          totalUnits: summary.totalQuantity,
          available: summary.totalAvailable,
          reserved: summary.totalReserved,
          damaged: 0,
        })),
      };
    }
    const response = await inventoryClient.request<ActualSummary | ActualItem>(`/v1/inventory/${encodeURIComponent(params.sku)}${query({ locationId: params.locationId })}`);
    if ("locations" in response) {
      const rows: InventoryStockRow[] = response.locations.map((row) => ({ sku: response.sku, productName: response.productName ?? null, location: location({ locationId: row.locationId, locationCode: row.locationCode, locationName: row.locationName }), locationCount: response.locations.length, totalUnits: row.quantity, available: row.available, reserved: row.reservedQuantity, damaged: 0 }));
      return page(rows, params);
    }
    return page([{ sku: response.sku, productName: response.productName ?? null, location: location({ locationId: response.locationId, locationCode: response.locationCode, locationName: response.locationName }), locationCount: 1, totalUnits: response.quantity, available: response.available, reserved: response.reservedQuantity, damaged: 0 }], params);
  },
  item: async (sku: string, locationId?: string) => {
    const response = await inventoryClient.request<ActualSummary | ActualItem>(`/v1/inventory/${encodeURIComponent(sku)}${query({ locationId })}`);
    if ("locations" in response) return response.locations[0] ? { id: response.locations[0].locationId, sku: response.sku, location: location({ locationId: response.locations[0].locationId, locationCode: response.locations[0].locationCode, locationName: response.locations[0].locationName }), quantity: response.locations[0].quantity, reservedQuantity: response.locations[0].reservedQuantity, available: response.locations[0].available } : await unsupported<InventoryItem>("No inventory exists for this SKU.");
    return item(response);
  },
  locations: () => inventoryClient.request<ActualLocation[]>("/v1/inventory/locations"),
  createLocation: (payload: { code: string; name: string; status?: LocationStatus }) => inventoryClient.json<InventoryLocation>("/v1/inventory/locations", payload),
  updateLocation: (id: string, payload: { code: string; name: string; status?: LocationStatus }) => inventoryClient.json<InventoryLocation>(`/v1/inventory/locations/${id}`, payload, { method: "PUT" }),
  deleteLocation: (id: string) => inventoryClient.request<void>(`/v1/inventory/locations/${id}`, { method: "DELETE" }),
  units: async (params: { page: number; size: number; sort: string; sku?: string; locationId?: string; status?: InventoryUnitStatus; serialNumber?: string; imei?: string; barcode?: string }): Promise<PageResponse<InventoryUnit>> => {
    const response = await inventoryClient.request<PageResponse<ActualUnit>>(`/v1/inventory/units${query({ page: params.page, size: params.size, sort: params.sort, sku: params.sku, locationId: params.locationId, status: params.status, serialNumber: params.serialNumber, imei: params.imei, barcode: params.barcode })}`);
    return { ...response, content: response.content.map(unit) };
  },
  unit: async (id: string) => ({ ...unit(await inventoryClient.request<ActualUnit>(`/v1/inventory/units/${id}`)), movements: [] as InventoryMovement[] }),
  receive: async (payload: InventoryReceivePayload) => { const response = await inventoryClient.json<ActualReceive, Omit<InventoryReceivePayload, "sku">>(`/v1/inventory/${encodeURIComponent(payload.sku)}/receive`, { locationId: payload.locationId, quantity: payload.quantity, referenceId: payload.referenceId, units: payload.units }); return response.units.map(unit); },
  reservations: async (params: { page: number; size: number; status?: ReservationStatus; sku?: string; locationId?: string; sort?: string }): Promise<PageResponse<InventoryReservation>> => {
    const response = await inventoryClient.request<PageResponse<ActualReservationList>>(`/v1/inventory/reservations${query({ page: params.page, size: params.size, status: params.status, sku: params.sku, locationId: params.locationId, sort: params.sort ?? "createdAt,desc" })}`);
    return { ...response, content: response.content.map(reservation) };
  },
  reservation: async (id: string) => reservation(await inventoryClient.request<ActualReservation>(`/v1/inventory/reservations/${id}`)),
  createReservation: async (payload: InventoryReservationPayload) => reservation(await inventoryClient.json<ActualReservation, Omit<InventoryReservationPayload, "sku">>(`/v1/inventory/${encodeURIComponent(payload.sku)}/reservations`, { locationId: payload.locationId, quantity: payload.quantity, referenceId: payload.referenceId, expiresAt: payload.expiresAt ? new Date(payload.expiresAt).toISOString() : undefined })),
  reservationAction: async (id: string, action: "confirm" | "release" | "cancel") => reservation(await inventoryClient.request<ActualReservation>(`/v1/inventory/reservations/${id}/${action}`, { method: "POST" })),
  adjustments: async (params: { page: number; size: number; sku?: string; locationId?: string; reason?: string }): Promise<PageResponse<InventoryAdjustment>> => {
    const response = await inventoryClient.request<PageResponse<ActualAdjustment>>(`/v1/inventory/adjustments${query({ page: params.page, size: params.size, sku: params.sku, reason: params.reason })}`);
    return { ...response, content: response.content.map(adjustment) };
  },
  createAdjustment: async (payload: InventoryAdjustmentPayload) => { const response = await inventoryClient.json<{ id: string; sku: string; locationId: string; quantity: number; reason: InventoryAdjustment["reason"]; referenceId: string | null; inventory: ActualItem; createdAt: string }, Omit<InventoryAdjustmentPayload, "sku">>(`/v1/inventory/${encodeURIComponent(payload.sku)}/adjustments`, { locationId: payload.locationId, quantity: payload.quantity, reason: payload.reason, referenceId: payload.referenceId, unitIds: payload.unitIds, units: payload.units }); return { id: response.id, sku: response.sku, location: location({ locationId: response.locationId }), quantity: response.quantity, reason: response.reason, referenceId: response.referenceId, createdAt: response.createdAt }; },
  transfers: async (params: { page: number; size: number; sku?: string; locationId?: string }): Promise<PageResponse<InventoryTransfer>> => { const response = await inventoryClient.request<PageResponse<ActualTransfer>>(`/v1/inventory/transfers${query({ page: params.page, size: params.size, sku: params.sku, locationId: params.locationId })}`); return { ...response, content: response.content.map(transfer) }; },
  createTransfer: async (payload: InventoryTransferPayload) => { await inventoryClient.json<void, InventoryTransferPayload>("/v1/inventory/transfers", payload); return { id: payload.referenceId, sku: payload.sku, fromLocation: location({ id: payload.fromLocationId }), toLocation: location({ id: payload.toLocationId }), unitIds: payload.unitIds, referenceId: payload.referenceId, createdAt: new Date().toISOString() } as InventoryTransfer; },
  reconciliation: async (sku: string, locationId: string) => { const response = await inventoryClient.request<{ sku: string; locationId: string; expectedQuantity: number; actualQuantity: number; expectedReserved: number; actualReserved: number; consistent: boolean }>(`/v1/inventory/${encodeURIComponent(sku)}/reconcile${query({ locationId })}`, { method: "POST" }); return { sku: response.sku, location: location({ id: response.locationId }), expected: response.expectedQuantity, actual: response.actualQuantity, difference: response.actualQuantity - response.expectedQuantity, status: response.consistent ? "CONSISTENT" : "DISCREPANCY" } satisfies InventoryReconciliationRow; },
};
