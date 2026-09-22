package com.shop.inventory.api;

import com.shop.inventory.adjustment.AdjustmentReason;
import com.shop.inventory.service.InventoryOperationsService;
import com.shop.inventory.service.ReservationService;
import com.shop.inventory.reservation.ReservationStatus;
import com.shop.inventory.unit.UnitStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@Tag(name = "Inventory", description = "Itemized inventory, unit, reservation, adjustment, and transfer APIs")
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryOperationsService inventory;
    private final ReservationService reservations;

    public InventoryController(InventoryOperationsService inventory, ReservationService reservations) {
        this.inventory = inventory;
        this.reservations = reservations;
    }

    @Operation(summary = "Get aggregate inventory for a SKU")
    @GetMapping("/{sku}")
    public Object getInventory(@PathVariable String sku,
                               @RequestParam(required = false) UUID locationId) {
        return inventory.getInventory(sku, locationId);
    }

    @Operation(summary = "Get customer-safe availability for a SKU")
    @GetMapping("/availability/{sku}")
    public InventoryDtos.AvailabilityResponse customerAvailability(@PathVariable String sku) {
        return inventory.customerAvailability(sku);
    }

    @Operation(summary = "Get global inventory dashboard totals")
    @GetMapping("/summary")
    public InventoryDtos.DashboardSummaryResponse dashboardSummary() {
        return inventory.dashboardSummary();
    }

    @Operation(summary = "List aggregate inventory by SKU")
    @GetMapping
    public Page<InventoryDtos.SummaryResponse> listInventory(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "sku,asc") String sort) {
        return inventory.listInventory(page, size, sort);
    }

    @Operation(summary = "Receive itemized inventory")
    @PostMapping("/{sku}/receive")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryDtos.ReceiveResponse receive(@PathVariable String sku,
                                                  @Valid @RequestBody InventoryDtos.ReceiveRequest request) {
        return inventory.receive(sku, request);
    }

    @Operation(summary = "List physical inventory units")
    @GetMapping("/{sku}/units")
    public Page<InventoryDtos.UnitResponse> listUnits(
            @PathVariable String sku,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UnitStatus status,
            @RequestParam(required = false) String serialNumber,
            @RequestParam(required = false) String imei,
            @RequestParam(required = false) String barcode,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, maximum 100", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field and direction, for example createdAt,asc", example = "createdAt,asc")
            @RequestParam(defaultValue = "createdAt,asc") String sort) {
        return inventory.listUnits(sku, locationId, status, serialNumber, imei, barcode, page, size, sort);
    }

    @Operation(summary = "Get one physical inventory unit")
    @GetMapping("/units/{unitId}")
    public InventoryDtos.UnitResponse getUnit(@PathVariable UUID unitId) {
        return inventory.getUnit(unitId);
    }

    @Operation(summary = "Apply a positive or explicit-unit negative inventory adjustment")
    @PostMapping("/{sku}/adjustments")
    public InventoryDtos.AdjustmentResponse adjust(@PathVariable String sku,
                                                   @Valid @RequestBody InventoryDtos.AdjustmentRequest request) {
        return inventory.adjust(sku, request);
    }

    @Operation(summary = "List inventory adjustment history")
    @GetMapping("/adjustments")
    public Page<InventoryDtos.AdjustmentListResponse> listAdjustments(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) AdjustmentReason reason,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, maximum 100", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return inventory.listAdjustments(sku, reason, page, size);
    }

    @Operation(summary = "List transfer history grouped by transfer reference")
    @GetMapping("/transfers")
    public Page<InventoryDtos.TransferResponse> listTransfers(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) UUID locationId,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, maximum 100", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return inventory.listTransfers(sku, locationId, page, size);
    }

    @Operation(summary = "Move explicit physical units between locations")
    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public void transfer(@Valid @RequestBody InventoryDtos.TransferRequest request) {
        inventory.transfer(request);
    }

    @Operation(summary = "Report aggregate versus itemized inventory without mutating stock")
    @PostMapping("/{sku}/reconcile")
    public InventoryDtos.ReconciliationResponse reconcile(@PathVariable String sku,
                                                          @RequestParam UUID locationId) {
        return inventory.reconcile(sku, locationId);
    }

    @Operation(summary = "Reserve available physical units")
    @PostMapping("/{sku}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryDtos.ReservationResponse reserve(@PathVariable String sku,
                                                     @Valid @RequestBody InventoryDtos.ReservationRequest request) {
        return reservations.reserve(sku, request);
    }

    @Operation(summary = "List reservations")
    @GetMapping("/reservations")
    public Page<InventoryDtos.ReservationListResponse> listReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) UUID locationId,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, maximum 100", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field and direction, for example createdAt,desc", example = "createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return reservations.list(status, sku, locationId, page, size, sort);
    }

    @Operation(summary = "Get a reservation")
    @GetMapping("/reservations/{reservationId}")
    public InventoryDtos.ReservationResponse getReservation(@PathVariable UUID reservationId) {
        return reservations.get(reservationId);
    }

    @Operation(summary = "Release a reservation")
    @PostMapping("/reservations/{reservationId}/release")
    public InventoryDtos.ReservationResponse release(@PathVariable UUID reservationId) {
        return reservations.release(reservationId);
    }

    @Operation(summary = "Confirm a reservation and mark its units SOLD")
    @PostMapping("/reservations/{reservationId}/confirm")
    public InventoryDtos.ReservationResponse confirm(@PathVariable UUID reservationId) {
        return reservations.confirm(reservationId);
    }

    @Operation(summary = "Cancel a reservation")
    @PostMapping("/reservations/{reservationId}/cancel")
    public InventoryDtos.ReservationResponse cancel(@PathVariable UUID reservationId) {
        return reservations.cancel(reservationId);
    }

    @Operation(summary = "Apply an Inventory-owned Shipping allocation transition",
            description = "Shipping may request ALLOCATE, IN_TRANSIT, or RELEASE for the exact reserved unit set. Inventory validates and mutates unit state.")
    @PostMapping("/reservations/{reservationId}/shipping-transition")
    public InventoryDtos.ReservationResponse shippingTransition(
            @PathVariable UUID reservationId,
            @Valid @RequestBody InventoryDtos.ShippingTransitionRequest request) {
        return reservations.shippingTransition(reservationId, request);
    }
}
