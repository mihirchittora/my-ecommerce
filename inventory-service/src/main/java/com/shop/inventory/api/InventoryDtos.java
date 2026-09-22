package com.shop.inventory.api;

import com.shop.inventory.adjustment.AdjustmentReason;
import com.shop.inventory.item.InventoryItem;
import com.shop.inventory.location.InventoryLocation;
import com.shop.inventory.movement.InventoryUnitMovement;
import com.shop.inventory.reservation.InventoryReservation;
import com.shop.inventory.unit.InventoryUnit;
import com.shop.inventory.unit.UnitStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class InventoryDtos {
    private InventoryDtos() {
    }

    public record UnitInput(
            @Size(max = 150) String serialNumber,
            @Size(max = 30) String imei,
            @Size(max = 150) String barcode) {
    }

    public record ReceiveRequest(
            @NotNull UUID locationId,
            @NotNull @Min(1) Integer quantity,
            @NotEmpty @Size(max = 10000) List<@Valid UnitInput> units,
            @NotBlank @Size(max = 200) String referenceId) {
    }

    public record AdjustmentRequest(
            @NotNull UUID locationId,
            @NotNull Long quantity,
            @NotNull AdjustmentReason reason,
            @Size(max = 200) String referenceId,
            @Size(max = 10000) List<UUID> unitIds,
            @Size(max = 10000) List<@Valid UnitInput> units) {
    }

    public record ReservationRequest(
            UUID locationId,
            @NotNull @Min(1) Long quantity,
            @NotBlank @Size(max = 200) String referenceId,
            @Future Instant expiresAt) {
    }

    public enum ShippingTransition {
        ALLOCATE,
        IN_TRANSIT,
        RELEASE
    }

    public record ShippingTransitionRequest(
            @NotNull ShippingTransition transition,
            @NotBlank @Size(max = 200) String shippingReference,
            @NotEmpty @Size(max = 10000) List<@NotNull UUID> unitIds) {
    }

    public record TransferRequest(
            @NotBlank @Size(max = 80) String sku,
            @NotNull UUID fromLocationId,
            @NotNull UUID toLocationId,
            @NotEmpty @Size(max = 10000) List<UUID> unitIds,
            @NotBlank @Size(max = 200) String referenceId) {
    }

    public record UnitResponse(
            UUID id,
            String unitCode,
            String sku,
            String productName,
            UUID locationId,
            String locationCode,
            String locationName,
            String serialNumber,
            String imei,
            String barcode,
            UnitStatus status,
            Instant receivedAt,
            Instant soldAt) {
        public static UnitResponse from(InventoryUnit unit) {
            return from(unit, null);
        }

        public static UnitResponse from(InventoryUnit unit, String productName) {
            InventoryItem item = unit.getInventoryItem();
            InventoryLocation location = item.getLocation();
            return new UnitResponse(
                    unit.getId(), unit.getUnitCode(), unit.getSku(), productName,
                    location.getId(), location.getCode(), location.getName(),
                    unit.getSerialNumber(), unit.getImei(), unit.getBarcode(),
                    unit.getStatus(), unit.getReceivedAt(), unit.getSoldAt());
        }
    }

    public record UnitBrief(UUID unitId, String unitCode, UnitStatus status) {
        public static UnitBrief from(InventoryUnit unit) {
            return new UnitBrief(unit.getId(), unit.getUnitCode(), unit.getStatus());
        }
    }

    public record ItemResponse(
            UUID id,
            String sku,
            String productName,
            UUID locationId,
            String locationCode,
            String locationName,
            long quantity,
            long reservedQuantity,
            long available,
            long version) {
        public static ItemResponse from(InventoryItem item) {
            return from(item, null);
        }

        public static ItemResponse from(InventoryItem item, String productName) {
            InventoryLocation location = item.getLocation();
            return new ItemResponse(item.getId(), item.getSku(), productName,
                    location.getId(), location.getCode(), location.getName(),
                    item.getQuantity(), item.getReservedQuantity(), item.available(), item.getVersion());
        }
    }

    public record ReceiveResponse(ItemResponse inventory, List<UnitResponse> units) {
    }

    public record LocationSummary(
            UUID locationId,
            String locationCode,
            String locationName,
            long quantity,
            long reservedQuantity,
            long available) {
        public static LocationSummary from(InventoryItem item) {
            InventoryLocation location = item.getLocation();
            return new LocationSummary(location.getId(), location.getCode(), location.getName(),
                    item.getQuantity(), item.getReservedQuantity(), item.available());
        }
    }

    public record SummaryResponse(
            String sku,
            String productName,
            long totalQuantity,
            long totalReserved,
            long totalAvailable,
            List<LocationSummary> locations) {
    }

    /** Customer-safe availability; operational quantities and locations stay private. */
    public record AvailabilityResponse(String sku, boolean available, String message) {
    }

    public record DashboardSummaryResponse(
            long totalInventoryUnits,
            long availableUnits,
            long reservedUnits,
            long damagedUnits,
            long activeLocations) {
    }

    public record AdjustmentResponse(
            UUID id,
            String sku,
            UUID locationId,
            long quantity,
            AdjustmentReason reason,
            String referenceId,
            ItemResponse inventory,
            Instant createdAt) {
    }

    public record AdjustmentListResponse(
            UUID id,
            String sku,
            UUID locationId,
            String locationCode,
            String locationName,
            long quantity,
            AdjustmentReason reason,
            String referenceId,
            Instant createdAt) {
        public static AdjustmentListResponse from(com.shop.inventory.adjustment.InventoryAdjustment adjustment) {
            InventoryLocation location = adjustment.getLocation();
            return new AdjustmentListResponse(
                    adjustment.getId(), adjustment.getSku(), location.getId(), location.getCode(), location.getName(),
                    adjustment.getQuantity(), adjustment.getReason(), adjustment.getReferenceId(), adjustment.getCreatedAt());
        }
    }

    public record TransferLocationResponse(UUID id, String code, String name, com.shop.inventory.location.LocationStatus status) {
        public static TransferLocationResponse from(InventoryLocation location) {
            return location == null ? null : new TransferLocationResponse(
                    location.getId(), location.getCode(), location.getName(), location.getStatus());
        }
    }

    public record TransferResponse(
            UUID id,
            String sku,
            TransferLocationResponse fromLocation,
            TransferLocationResponse toLocation,
            List<UUID> unitIds,
            String referenceId,
            Instant createdAt) {
        public static TransferResponse from(List<InventoryUnitMovement> movements) {
            InventoryUnitMovement first = movements.get(0);
            Instant createdAt = movements.stream()
                    .map(InventoryUnitMovement::getCreatedAt)
                    .min(Comparator.naturalOrder())
                    .orElse(first.getCreatedAt());
            return new TransferResponse(
                    first.getId(),
                    first.getInventoryUnit().getSku(),
                    TransferLocationResponse.from(first.getFromLocation()),
                    TransferLocationResponse.from(first.getToLocation()),
                    movements.stream().map(movement -> movement.getInventoryUnit().getId()).distinct().toList(),
                    first.getReferenceId(),
                    createdAt);
        }
    }

    public record ReservationResponse(
            UUID reservationId,
            String sku,
            UUID locationId,
            String locationCode,
            long quantity,
            String referenceId,
            com.shop.inventory.reservation.ReservationStatus status,
            Instant expiresAt,
            List<UnitBrief> units,
            Instant createdAt,
            Instant updatedAt) {
        public static ReservationResponse from(InventoryReservation reservation, List<InventoryUnit> units) {
            InventoryLocation location = reservation.getLocation();
            return new ReservationResponse(reservation.getId(), reservation.getSku(),
                    location.getId(), location.getCode(), reservation.getQuantity(),
                    reservation.getReferenceId(), reservation.getStatus(), reservation.getExpiresAt(),
                    units.stream().map(UnitBrief::from).toList(),
                    reservation.getCreatedAt(), reservation.getUpdatedAt());
        }
    }

    public record ReservationListResponse(
            UUID reservationId,
            String sku,
            UUID locationId,
            String locationCode,
            long quantity,
            String referenceId,
            com.shop.inventory.reservation.ReservationStatus status,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt) {
        public static ReservationListResponse from(InventoryReservation reservation) {
            InventoryLocation location = reservation.getLocation();
            return new ReservationListResponse(
                    reservation.getId(), reservation.getSku(), location.getId(), location.getCode(),
                    reservation.getQuantity(), reservation.getReferenceId(), reservation.getStatus(),
                    reservation.getExpiresAt(), reservation.getCreatedAt(), reservation.getUpdatedAt());
        }
    }

    public record ReconciliationResponse(
            String sku,
            UUID locationId,
            long expectedQuantity,
            long actualQuantity,
            long expectedReserved,
            long actualReserved,
            boolean consistent) {
    }
}
