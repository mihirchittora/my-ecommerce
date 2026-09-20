package com.shop.inventory.service;

import com.shop.inventory.adjustment.AdjustmentReason;
import com.shop.inventory.adjustment.AdjustmentRepository;
import com.shop.inventory.adjustment.InventoryAdjustment;
import com.shop.inventory.api.InventoryDtos;
import com.shop.inventory.catalog.CatalogSkuLookup;
import com.shop.inventory.catalog.CatalogSkuResponse;
import com.shop.inventory.common.BadRequestException;
import com.shop.inventory.common.ConflictException;
import com.shop.inventory.common.NotFoundException;
import com.shop.inventory.item.InventoryItem;
import com.shop.inventory.item.InventoryItemRepository;
import com.shop.inventory.location.InventoryLocation;
import com.shop.inventory.location.LocationService;
import com.shop.inventory.movement.InventoryUnitMovement;
import com.shop.inventory.movement.MovementRepository;
import com.shop.inventory.unit.InventoryUnit;
import com.shop.inventory.unit.InventoryUnitRepository;
import com.shop.inventory.unit.UnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class InventoryOperationsService {
    private static final List<UnitStatus> DISPOSED_STATUSES = List.of(UnitStatus.SOLD, UnitStatus.DAMAGED, UnitStatus.LOST);

    private final CatalogSkuLookup catalog;
    private final LocationService locations;
    private final InventoryItemRepository items;
    private final InventoryUnitRepository units;
    private final AdjustmentRepository adjustments;
    private final MovementRepository movements;

    public InventoryOperationsService(CatalogSkuLookup catalog,
                                      LocationService locations,
                                      InventoryItemRepository items,
                                      InventoryUnitRepository units,
                                      AdjustmentRepository adjustments,
                                      MovementRepository movements) {
        this.catalog = catalog;
        this.locations = locations;
        this.items = items;
        this.units = units;
        this.adjustments = adjustments;
        this.movements = movements;
    }

    @Transactional
    public InventoryDtos.ReceiveResponse receive(String rawSku, InventoryDtos.ReceiveRequest request) {
        String sku = normalizeSku(rawSku);
        CatalogSkuResponse catalogSku = catalog.requireActive(sku);
        String referenceId = normalizeReference(request.referenceId());
        InventoryLocation location = activeLocation(request.locationId());
        if (request.quantity() != request.units().size()) {
            throw new BadRequestException("quantity must equal the number of supplied units");
        }
        validateUnitMetadata(request.units());

        List<InventoryUnit> existing = units.findBySkuAndReceiptReferenceIdOrderByCreatedAt(sku, referenceId);
        if (!existing.isEmpty()) {
            if (existing.size() != request.quantity()
                    || !existing.get(0).getInventoryItem().getLocation().getId().equals(location.getId())) {
                throw new ConflictException("referenceId was already used for a different receipt");
            }
            return new InventoryDtos.ReceiveResponse(
                    InventoryDtos.ItemResponse.from(existing.get(0).getInventoryItem(), catalogSku.productName()),
                    existing.stream().map(unit -> InventoryDtos.UnitResponse.from(unit, catalogSku.productName())).toList());
        }

        InventoryItem item = lockOrCreateItem(sku, location);
        item.setQuantity(item.getQuantity() + request.quantity());
        List<InventoryUnit> created = new ArrayList<>();
        for (InventoryDtos.UnitInput input : request.units()) {
            created.add(newUnit(item, input, referenceId));
        }
        units.saveAll(created);
        created.forEach(unit -> recordMovement(unit, null, location, null, UnitStatus.AVAILABLE,
                "RECEIVE", referenceId, "Inventory received"));
        InventoryAdjustment adjustment = newAdjustment(sku, location, request.quantity(),
                AdjustmentReason.PURCHASE_RECEIPT, referenceId);
        adjustments.save(adjustment);
        return new InventoryDtos.ReceiveResponse(
                InventoryDtos.ItemResponse.from(item, catalogSku.productName()),
                created.stream().map(unit -> InventoryDtos.UnitResponse.from(unit, catalogSku.productName())).toList());
    }

    @Transactional(readOnly = true)
    public Object getInventory(String rawSku, UUID locationId) {
        String sku = normalizeSku(rawSku);
        CatalogSkuResponse catalogSku = catalog.requireActive(sku);
        if (locationId != null) {
            return InventoryDtos.ItemResponse.from(items.findBySkuAndLocation_Id(sku, locationId)
                    .orElseThrow(() -> new NotFoundException("No inventory exists for SKU at location")),
                    catalogSku.productName());
        }
        List<InventoryItem> inventory = items.findBySkuOrderByLocation_Code(sku);
        return new InventoryDtos.SummaryResponse(sku, catalogSku.productName(),
                inventory.stream().mapToLong(InventoryItem::getQuantity).sum(),
                inventory.stream().mapToLong(InventoryItem::getReservedQuantity).sum(),
                inventory.stream().mapToLong(InventoryItem::available).sum(),
                inventory.stream().map(InventoryDtos.LocationSummary::from).toList());
    }

    @Transactional(readOnly = true)
    public InventoryDtos.DashboardSummaryResponse dashboardSummary() {
        return new InventoryDtos.DashboardSummaryResponse(
                units.countByStatusNotIn(DISPOSED_STATUSES),
                units.countByStatus(UnitStatus.AVAILABLE),
                units.countByStatus(UnitStatus.RESERVED),
                units.countByStatus(UnitStatus.DAMAGED),
                locations.countActive());
    }

    @Transactional(readOnly = true)
    public Page<InventoryDtos.SummaryResponse> listInventory(int page, int size, String sort) {
        List<String> skus = new ArrayList<>(items.findDistinctSkusOrderBySku());
        if (sort != null && sort.toLowerCase(Locale.ROOT).contains("desc")) {
            java.util.Collections.reverse(skus);
        }
        int total = skus.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);
        List<InventoryDtos.SummaryResponse> content = skus.subList(start, end).stream()
                .map(this::summaryForSku)
                .toList();
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    @Transactional(readOnly = true)
    public Page<InventoryDtos.UnitResponse> listUnits(String rawSku, UUID locationId, UnitStatus status,
                                                       String serialNumber, String imei, String barcode,
                                                       int page, int size, String sort) {
        String sku = normalizeSku(rawSku);
        CatalogSkuResponse catalogSku = catalog.requireActive(sku);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
        return units.search(sku, locationId, status == null ? null : status.name(),
                        blankToNull(serialNumber), blankToNull(imei), blankToNull(barcode), pageable)
                .map(unit -> InventoryDtos.UnitResponse.from(unit, catalogSku.productName()));
    }

    @Transactional(readOnly = true)
    public InventoryDtos.UnitResponse getUnit(UUID unitId) {
        InventoryUnit unit = units.findById(unitId)
                .orElseThrow(() -> new NotFoundException("Inventory unit not found: " + unitId));
        CatalogSkuResponse catalogSku = catalog.requireActive(unit.getSku());
        return InventoryDtos.UnitResponse.from(unit, catalogSku.productName());
    }

    @Transactional
    public InventoryDtos.AdjustmentResponse adjust(String rawSku, InventoryDtos.AdjustmentRequest request) {
        String sku = normalizeSku(rawSku);
        catalog.requireActive(sku);
        InventoryLocation location = activeLocation(request.locationId());
        if (request.quantity() == 0) throw new BadRequestException("quantity cannot be zero");
        String referenceId = normalizeNullableReference(request.referenceId());

        if (referenceId != null) {
            var existing = adjustments.findFirstBySkuAndLocation_IdAndReferenceIdOrderByCreatedAtAsc(sku, location.getId(), referenceId);
            if (existing.isPresent()) {
                InventoryItem item = items.findBySkuAndLocation_Id(sku, location.getId())
                        .orElseThrow(() -> new NotFoundException("Inventory item not found"));
                return adjustmentResponse(existing.get(), item);
            }
        }

        InventoryItem item = lockOrCreateItem(sku, location);
        long quantity = request.quantity();
        if (quantity > 0) {
            if (request.units() == null || request.units().size() != quantity) {
                throw new BadRequestException("positive adjustment requires units matching quantity");
            }
            if (request.reason() == AdjustmentReason.DAMAGE || request.reason() == AdjustmentReason.LOSS) {
                throw new BadRequestException("DAMAGE and LOSS adjustments must have a negative quantity");
            }
            validateUnitMetadata(request.units());
            item.setQuantity(item.getQuantity() + quantity);
            List<InventoryUnit> created = request.units().stream()
                    .map(input -> newUnit(item, input, referenceId))
                    .toList();
            units.saveAll(created);
            created.forEach(unit -> recordMovement(unit, null, location, null, UnitStatus.AVAILABLE,
                    "ADJUSTMENT", referenceId, request.reason().name()));
        } else {
            long amount = safeAbsolute(quantity);
            if (request.unitIds() == null || request.unitIds().size() != amount
                    || new HashSet<>(request.unitIds()).size() != request.unitIds().size()) {
                throw new BadRequestException("negative adjustment requires distinct unitIds matching absolute quantity");
            }
            List<InventoryUnit> selected = units.findAllByIdInForUpdate(request.unitIds());
            if (selected.size() != request.unitIds().size()) throw new NotFoundException("One or more inventory units were not found");
            for (InventoryUnit unit : selected) {
                if (!unit.getInventoryItem().getId().equals(item.getId())) {
                    throw new ConflictException("All adjustment units must belong to the requested SKU and location");
                }
                if (unit.getStatus() != UnitStatus.AVAILABLE) {
                    throw new ConflictException("Only AVAILABLE units can be removed by adjustment");
                }
                UnitStatus disposedStatus = request.reason() == AdjustmentReason.LOSS ? UnitStatus.LOST : UnitStatus.DAMAGED;
                unit.setStatus(disposedStatus);
                recordMovement(unit, location, location, UnitStatus.AVAILABLE, disposedStatus,
                        "ADJUSTMENT", referenceId, request.reason().name());
            }
            item.setQuantity(item.getQuantity() - amount);
        }

        InventoryAdjustment adjustment = newAdjustment(sku, location, quantity, request.reason(), referenceId);
        adjustments.save(adjustment);
        return adjustmentResponse(adjustment, item);
    }

    @Transactional(readOnly = true)
    public Page<InventoryDtos.AdjustmentListResponse> listAdjustments(String rawSku, AdjustmentReason reason,
                                                                       int page, int size) {
        String sku = rawSku == null || rawSku.isBlank() ? null : normalizeSku(rawSku);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InventoryAdjustment> result;
        if (sku != null && reason != null) {
            result = adjustments.findBySkuAndReasonOrderByCreatedAtDesc(sku, reason, pageable);
        } else if (sku != null) {
            result = adjustments.findBySkuOrderByCreatedAtDesc(sku, pageable);
        } else if (reason != null) {
            result = adjustments.findByReasonOrderByCreatedAtDesc(reason, pageable);
        } else {
            result = adjustments.findAllByOrderByCreatedAtDesc(pageable);
        }
        return result.map(InventoryDtos.AdjustmentListResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<InventoryDtos.TransferResponse> listTransfers(String rawSku, UUID locationId, int page, int size) {
        String sku = rawSku == null || rawSku.isBlank() ? null : normalizeSku(rawSku);
        List<InventoryUnitMovement> movementList = sku == null
                ? movements.findByReferenceTypeOrderByCreatedAtDesc("TRANSFER")
                : movements.findByReferenceTypeAndInventoryUnit_SkuOrderByCreatedAtDesc("TRANSFER", sku);

        Map<String, List<InventoryUnitMovement>> grouped = new LinkedHashMap<>();
        for (InventoryUnitMovement movement : movementList) {
            if (locationId != null && !touchesLocation(movement, locationId)) {
                continue;
            }
            String key = movement.getReferenceId() == null
                    ? movement.getId().toString()
                    : movement.getReferenceId();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(movement);
        }

        List<List<InventoryUnitMovement>> transfers = new ArrayList<>(grouped.values());
        transfers.sort(Comparator.comparing(this::latestMovementTime).reversed());
        int total = transfers.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);
        List<InventoryDtos.TransferResponse> content = transfers.subList(start, end).stream()
                .map(InventoryDtos.TransferResponse::from)
                .toList();
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    @Transactional
    public void transfer(InventoryDtos.TransferRequest request) {
        String sku = normalizeSku(request.sku());
        catalog.requireActive(sku);
        String referenceId = normalizeReference(request.referenceId());
        if (request.fromLocationId().equals(request.toLocationId())) {
            throw new BadRequestException("fromLocationId and toLocationId must be different");
        }
        if (new HashSet<>(request.unitIds()).size() != request.unitIds().size()) {
            throw new BadRequestException("unitIds must be distinct");
        }
        List<InventoryUnitMovement> previous = movements.findByReferenceTypeAndReferenceId("TRANSFER", referenceId);
        if (!previous.isEmpty()) {
            Set<UUID> previousUnitIds = previous.stream().map(movement -> movement.getInventoryUnit().getId()).collect(java.util.stream.Collectors.toSet());
            if (!previousUnitIds.equals(new HashSet<>(request.unitIds()))) {
                throw new ConflictException("referenceId was already used for a different transfer");
            }
            return;
        }
        InventoryLocation from = activeLocation(request.fromLocationId());
        InventoryLocation to = activeLocation(request.toLocationId());
        InventoryItem source = items.findForUpdate(sku, from.getId())
                .orElseThrow(() -> new NotFoundException("Source inventory does not exist"));
        InventoryItem destination = items.findForUpdate(sku, to.getId()).orElse(null);
        if (destination == null) {
            destination = newItem(sku, to);
            items.saveAndFlush(destination);
        }
        List<InventoryUnit> selected = units.findAllByIdInForUpdate(request.unitIds());
        if (selected.size() != request.unitIds().size()) throw new NotFoundException("One or more inventory units were not found");
        for (InventoryUnit unit : selected) {
            if (!unit.getInventoryItem().getId().equals(source.getId()) || !unit.getSku().equals(sku)) {
                throw new ConflictException("All transfer units must belong to the source SKU and location");
            }
            if (unit.getStatus() != UnitStatus.AVAILABLE) {
                throw new ConflictException("Only AVAILABLE units can be transferred");
            }
            unit.setInventoryItem(destination);
            recordMovement(unit, from, to, UnitStatus.AVAILABLE, UnitStatus.AVAILABLE,
                    "TRANSFER", referenceId, "Inventory transfer");
        }
        source.setQuantity(source.getQuantity() - selected.size());
        destination.setQuantity(destination.getQuantity() + selected.size());
    }

    @Transactional(readOnly = true)
    public InventoryDtos.ReconciliationResponse reconcile(String rawSku, UUID locationId) {
        String sku = normalizeSku(rawSku);
        catalog.requireActive(sku);
        InventoryItem item = items.findBySkuAndLocation_Id(sku, locationId)
                .orElseThrow(() -> new NotFoundException("Inventory item not found"));
        long actualQuantity = units.countActiveByItemId(item.getId(), DISPOSED_STATUSES);
        long actualReserved = units.countByInventoryItem_IdAndStatus(item.getId(), UnitStatus.RESERVED);
        return new InventoryDtos.ReconciliationResponse(sku, locationId, item.getQuantity(), actualQuantity,
                item.getReservedQuantity(), actualReserved,
                item.getQuantity() == actualQuantity && item.getReservedQuantity() == actualReserved);
    }

    private InventoryLocation activeLocation(UUID id) {
        InventoryLocation location = locations.find(id);
        if (location.getStatus() != com.shop.inventory.location.LocationStatus.ACTIVE) {
            throw new ConflictException("Location is inactive: " + location.getCode());
        }
        return location;
    }

    private InventoryDtos.SummaryResponse summaryForSku(String sku) {
        CatalogSkuResponse catalogSku = catalog.requireActive(sku);
        List<InventoryItem> inventory = items.findBySkuOrderByLocation_Code(sku);
        return new InventoryDtos.SummaryResponse(sku, catalogSku.productName(),
                inventory.stream().mapToLong(InventoryItem::getQuantity).sum(),
                inventory.stream().mapToLong(InventoryItem::getReservedQuantity).sum(),
                inventory.stream().mapToLong(InventoryItem::available).sum(),
                inventory.stream().map(InventoryDtos.LocationSummary::from).toList());
    }

    private InventoryItem lockOrCreateItem(String sku, InventoryLocation location) {
        return items.findForUpdate(sku, location.getId()).orElseGet(() -> {
            InventoryItem item = newItem(sku, location);
            return items.saveAndFlush(item);
        });
    }

    private InventoryItem newItem(String sku, InventoryLocation location) {
        InventoryItem item = new InventoryItem();
        item.setSku(sku);
        item.setLocation(location);
        item.setQuantity(0);
        item.setReservedQuantity(0);
        return item;
    }

    private InventoryUnit newUnit(InventoryItem item, InventoryDtos.UnitInput input, String referenceId) {
        InventoryUnit unit = new InventoryUnit();
        unit.setInventoryItem(item);
        unit.setSku(item.getSku());
        unit.setSerialNumber(blankToNull(input.serialNumber()));
        unit.setImei(blankToNull(input.imei()));
        unit.setBarcode(blankToNull(input.barcode()));
        unit.setReceiptReferenceId(referenceId);
        unit.setStatus(UnitStatus.AVAILABLE);
        return unit;
    }

    private InventoryAdjustment newAdjustment(String sku, InventoryLocation location, long quantity,
                                              AdjustmentReason reason, String referenceId) {
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setSku(sku);
        adjustment.setLocation(location);
        adjustment.setQuantity(quantity);
        adjustment.setReason(reason);
        adjustment.setReferenceId(referenceId);
        return adjustment;
    }

    private InventoryDtos.AdjustmentResponse adjustmentResponse(InventoryAdjustment adjustment, InventoryItem item) {
        return new InventoryDtos.AdjustmentResponse(adjustment.getId(), adjustment.getSku(),
                adjustment.getLocation().getId(), adjustment.getQuantity(), adjustment.getReason(),
                adjustment.getReferenceId(), InventoryDtos.ItemResponse.from(item), adjustment.getCreatedAt());
    }

    private void recordMovement(InventoryUnit unit, InventoryLocation from, InventoryLocation to,
                                UnitStatus fromStatus, UnitStatus toStatus,
                                String referenceType, String referenceId, String notes) {
        InventoryUnitMovement movement = new InventoryUnitMovement();
        movement.setInventoryUnit(unit);
        movement.setFromLocation(from);
        movement.setToLocation(to);
        movement.setFromStatus(fromStatus);
        movement.setToStatus(toStatus);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setNotes(notes);
        movements.save(movement);
    }

    private boolean touchesLocation(InventoryUnitMovement movement, UUID locationId) {
        return (movement.getFromLocation() != null && locationId.equals(movement.getFromLocation().getId()))
                || (movement.getToLocation() != null && locationId.equals(movement.getToLocation().getId()));
    }

    private Instant latestMovementTime(List<InventoryUnitMovement> movementList) {
        return movementList.stream()
                .map(InventoryUnitMovement::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(Instant.MIN);
    }

    private void validateUnitMetadata(List<InventoryDtos.UnitInput> input) {
        Set<String> serials = new HashSet<>();
        Set<String> imeis = new HashSet<>();
        Set<String> barcodes = new HashSet<>();
        for (InventoryDtos.UnitInput unit : input) {
            String serial = blankToNull(unit.serialNumber());
            String imei = blankToNull(unit.imei());
            String barcode = blankToNull(unit.barcode());
            if (serial != null && !serials.add(serial.toUpperCase(Locale.ROOT))) throw new ConflictException("Duplicate serialNumber in request");
            if (imei != null && !imeis.add(imei)) throw new ConflictException("Duplicate IMEI in request");
            if (barcode != null && !barcodes.add(barcode.toUpperCase(Locale.ROOT))) throw new ConflictException("Duplicate barcode in request");
        }
    }

    private Sort.Order[] parseSort(String sort) {
        if (sort == null || sort.isBlank()) return new Sort.Order[]{Sort.Order.asc("createdAt")};
        List<Sort.Order> orders = new ArrayList<>();
        String[] expressions = sort.split(",");
        for (int index = 0; index < expressions.length; index += 2) {
            String field = expressions[index].trim();
            if (!Set.of("createdAt", "unitCode", "status", "serialNumber", "imei", "barcode").contains(field)) {
                throw new BadRequestException("Unsupported unit sort field: " + field);
            }
            String direction = index + 1 < expressions.length ? expressions[index + 1].trim() : "asc";
            if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
                throw new BadRequestException("Sort direction must be asc or desc");
            }
            String databaseField = switch (field) {
                case "createdAt" -> "created_at";
                case "unitCode" -> "unit_code";
                case "serialNumber" -> "serial_number";
                default -> field;
            };
            orders.add(new Sort.Order(Sort.Direction.fromString(direction), databaseField));
        }
        return orders.toArray(Sort.Order[]::new);
    }

    private String normalizeSku(String sku) {
        String normalized = sku == null ? "" : sku.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) throw new BadRequestException("SKU must not be blank");
        return normalized;
    }

    private String normalizeReference(String reference) {
        String normalized = normalizeNullableReference(reference);
        if (normalized == null) throw new BadRequestException("referenceId must not be blank");
        return normalized;
    }

    private String normalizeNullableReference(String reference) {
        return blankToNull(reference);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private long safeAbsolute(long value) {
        if (value == Long.MIN_VALUE) throw new BadRequestException("quantity is out of range");
        return Math.abs(value);
    }
}
