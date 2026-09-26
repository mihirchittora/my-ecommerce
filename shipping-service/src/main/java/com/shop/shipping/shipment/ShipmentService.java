package com.shop.shipping.shipment;

import com.shop.shipping.api.ShippingDtos;
import com.shop.shipping.carrier.CarrierGateway;
import com.shop.shipping.common.BadRequestException;
import com.shop.shipping.common.ConflictException;
import com.shop.shipping.common.DependencyException;
import com.shop.shipping.common.NotFoundException;
import com.shop.shipping.fulfillment.FulfillmentEntity;
import com.shop.shipping.fulfillment.FulfillmentItemEntity;
import com.shop.shipping.fulfillment.FulfillmentService;
import com.shop.shipping.fulfillment.FulfillmentStatus;
import com.shop.shipping.inventory.InventoryClient;
import com.shop.shipping.order.OrderClient;
import com.shop.shipping.security.SecurityAccess;
import com.shop.shipping.tracking.TrackingEventEntity;
import com.shop.shipping.tracking.TrackingEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ShipmentService {
    private final ShipmentRepository shipments;
    private final ShipmentItemRepository items;
    private final ShipmentHistoryRepository history;
    private final ShipmentIdempotencyRepository idempotency;
    private final TrackingEventRepository tracking;
    private final FulfillmentService fulfillmentService;
    private final ShipmentPersistenceService persistence;
    private final ShipmentNumberGenerator numberGenerator;
    private final OrderClient orderClient;
    private final InventoryClient inventoryClient;
    private final CarrierGateway carrier;

    public ShipmentService(ShipmentRepository shipments, ShipmentItemRepository items,
                           ShipmentHistoryRepository history, ShipmentIdempotencyRepository idempotency,
                           TrackingEventRepository tracking, FulfillmentService fulfillmentService,
                           ShipmentPersistenceService persistence, ShipmentNumberGenerator numberGenerator,
                           OrderClient orderClient, InventoryClient inventoryClient, CarrierGateway carrier) {
        this.shipments = shipments;
        this.items = items;
        this.history = history;
        this.idempotency = idempotency;
        this.tracking = tracking;
        this.fulfillmentService = fulfillmentService;
        this.persistence = persistence;
        this.numberGenerator = numberGenerator;
        this.orderClient = orderClient;
        this.inventoryClient = inventoryClient;
        this.carrier = carrier;
    }

    public ShipmentEntity create(ShippingDtos.CreateShipmentRequest request, String idempotencyKey,
                                 Authentication authentication) {
        SecurityAccess.require(authentication, "SHIPPING_CREATE");
        return createInternal(request, idempotencyKey);
    }

    public ShipmentEntity createInternal(ShippingDtos.CreateShipmentRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 200) {
            throw new BadRequestException("Idempotency-Key is required and must be at most 200 characters");
        }
        FulfillmentEntity fulfillment = fulfillmentService.load(request.fulfillmentId());
        if (fulfillment.getStatus() == FulfillmentStatus.CANCELLED || fulfillment.getStatus() == FulfillmentStatus.COMPLETED) {
            throw new ConflictException("Fulfillment cannot accept a new shipment in " + fulfillment.getStatus());
        }
        String hash = sha256(request.toString());
        ShipmentIdempotencyEntity prior = idempotency.findByFulfillment_IdAndIdempotencyKey(fulfillment.getId(), idempotencyKey).orElse(null);
        if (prior != null) {
            if (!hash.equals(prior.getRequestHash())) throw new ConflictException("Idempotency-Key was reused with a different request");
            ShipmentEntity priorShipment = prior.getShipment();
            if (priorShipment == null) throw new ConflictException("Shipment creation is still in progress");
            if (priorShipment.getStatus() != ShipmentStatus.FAILED) return priorShipment;
        }

        OrderClient.OrderSnapshot order = orderClient.getOrder(fulfillment.getOrderId());
        if (!Set.of("CONFIRMED", "FULFILLING").contains(order.status())) {
            throw new ConflictException("Order must be CONFIRMED or FULFILLING before shipment creation");
        }
        List<PreparedItem> prepared = prepareItems(fulfillment, request.lines());
        validateInventoryReferences(fulfillment, prepared);
        String carrierName = request.carrier().trim().toUpperCase(Locale.ROOT);
        if (!carrier.name().equals(carrierName)) throw new BadRequestException("Unsupported carrier: " + carrierName);
        String currency = request.currency() == null || request.currency().isBlank()
                ? order.currency() : request.currency().trim().toUpperCase(Locale.ROOT);
        if (order.currency() != null && !order.currency().equalsIgnoreCase(currency)) throw new ConflictException("Shipment currency must match the Order currency");
        BigDecimal cost = request.shippingCost() == null ? BigDecimal.ZERO : request.shippingCost();

        if ("CONFIRMED".equals(order.status())) {
            // No carrier shipment number exists yet. Use the stable Order number
            // for the idempotent FULFILLING milestone instead of constructing a
            // value longer than Order's shipment-reference contract allows.
            orderClient.notifyShipment(order.id(), fulfillment.getOrderNumber(), "FULFILLING");
        }

        ShipmentEntity shipment = prior == null
                ? persistence.prepare(fulfillment, numberGenerator.next(), carrierName,
                request.serviceLevel().trim().toUpperCase(Locale.ROOT), cost, currency, idempotencyKey, hash, prepared)
                : persistence.retryFailed(prior.getShipment());
        List<InventoryAllocation> allocations = new ArrayList<>();
        try {
            allocateInventory(shipment, prepared, allocations);
        } catch (RuntimeException ex) {
            releaseInventory(allocations);
            persistence.markCarrierFailure(shipment.getId(), "Inventory Shipping allocation failed");
            throw ex;
        }
        try {
            CarrierGateway.CarrierShipmentResponse response = carrier.createShipment(
                    new CarrierGateway.CarrierShipmentRequest(shipment.getShipmentNumber(), shipment.getServiceLevel(),
                            shipment.getCurrency(), shipment.getShipmentNumber(), shipment.getPackageCount(),
                            new CarrierGateway.CarrierAddress(fulfillment.getShippingRecipientName(), fulfillment.getShippingPhone(),
                                    fulfillment.getShippingLine1(), fulfillment.getShippingLine2(), fulfillment.getShippingCity(),
                                    fulfillment.getShippingState(), fulfillment.getShippingPostalCode(), fulfillment.getShippingCountry())));
            if (response == null || response.providerShipmentId() == null || response.providerShipmentId().isBlank()
                    || response.trackingNumber() == null || response.trackingNumber().isBlank()) {
                throw new DependencyException("Carrier returned an incomplete shipment response");
            }
            shipment = persistence.markCarrierSuccess(shipment.getId(), response);
        } catch (RuntimeException ex) {
            releaseInventory(allocations);
            persistence.markCarrierFailure(shipment.getId(), "Carrier shipment creation failed");
            if (ex instanceof com.shop.shipping.common.ShippingApiException api && api.status().is5xxServerError()) throw api;
            throw new DependencyException("Carrier shipment creation failed", ex);
        }

        for (InventoryAllocation allocation : allocations) {
            inventoryClient.shippingTransition(allocation.reservationId(), "IN_TRANSIT", shipment.getShipmentNumber(), allocation.unitIds());
        }

        refreshFulfillmentAfterShipment(shipment);
        if (isFulfillmentFullyShipped(fulfillment.getId())) notifyOrder(shipment, "SHIPPED");
        return shipment;
    }

    public Page<ShippingDtos.ShipmentSummaryResponse> list(String search, ShipmentStatus status, String carrierName,
                                                           Instant createdFrom, Instant createdTo,
                                                           int page, int size, String sort, Authentication authentication) {
        SecurityAccess.require(authentication, "SHIPPING_READ");
        Specification<ShipmentEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("shipmentNumber")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("orderNumber")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("trackingNumber")), pattern)));
        }
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status));
        }
        if (carrierName != null && !carrierName.isBlank()) {
            String normalizedCarrier = carrierName.trim().toUpperCase(Locale.ROOT);
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("carrier"), normalizedCarrier));
        }
        if (createdFrom != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }
        return shipments.findAll(specification, pageable(page, size, sort)).map(ShippingDtos.ShipmentSummaryResponse::from);
    }

    public Page<ShippingDtos.ShipmentSummaryResponse> my(int page, int size, String sort, Authentication authentication) {
        String subject = SecurityAccess.subject(authentication);
        return shipments.findByCustomerId(subject, pageable(page, size, sort)).map(ShippingDtos.ShipmentSummaryResponse::from);
    }

    public ShippingDtos.ShipmentResponse get(UUID id, Authentication authentication) {
        ShipmentEntity shipment = persistence.load(id);
        assertVisible(shipment, authentication);
        boolean operational = SecurityAccess.has(authentication, "SHIPPING_READ");
        return response(shipment, operational);
    }

    public ShippingDtos.ShipmentResponse detailInternal(UUID id) {
        return response(persistence.load(id), true);
    }

    public ShippingDtos.TrackingResponse tracking(UUID id, Authentication authentication) {
        ShipmentEntity shipment = persistence.load(id);
        assertVisible(shipment, authentication);
        if (SecurityAccess.has(authentication, "SHIPPING_READ")) {
            SecurityAccess.require(authentication, "SHIPPING_TRACK");
        }
        return new ShippingDtos.TrackingResponse(shipment.getShipmentNumber(), shipment.getCarrier(), shipment.getTrackingNumber(),
                shipment.getStatus(), shipment.getEstimatedDeliveryAt(), tracking.findByShipment_IdOrderByOccurredAtAsc(id)
                .stream().map(ShippingDtos.TrackingEventResponse::from).toList());
    }

    public ShipmentEntity cancel(UUID id, Authentication authentication) {
        SecurityAccess.require(authentication, "SHIPPING_CANCEL");
        ShipmentEntity shipment = persistence.load(id);
        if (!SecurityAccess.has(authentication, "SHIPPING_READ") && !SecurityAccess.subject(authentication).equals(shipment.getCustomerId())) {
            throw new NotFoundException("Shipment not found: " + id);
        }
        if (shipment.getStatus() == ShipmentStatus.CANCELLED) return shipment;
        if (!ShipmentStateMachine.cancellable(shipment.getStatus())) throw new ConflictException("Shipment cannot be cancelled after " + shipment.getStatus());
        if (shipment.getProviderShipmentId() != null) carrier.cancelShipment(shipment.getProviderShipmentId(), shipment.getShipmentNumber());
        return persistence.cancel(id);
    }

    public ShipmentEntity retryOrderNotification(UUID id, Authentication authentication) {
        SecurityAccess.require(authentication, "SHIPPING_MANAGE");
        ShipmentEntity shipment = persistence.load(id);
        if (!shipment.isOrderNotificationPending()) return shipment;
        notifyOrder(shipment, shipment.getStatus() == ShipmentStatus.DELIVERED ? "DELIVERED" : "SHIPPED");
        return persistence.load(id);
    }

    public List<PreparedItem> prepareItems(FulfillmentEntity fulfillment, List<ShippingDtos.ShipmentLineRequest> lines) {
        List<FulfillmentItemEntity> fulfillmentItems = fulfillmentService.itemEntities(fulfillment.getId());
        Map<UUID, FulfillmentItemEntity> byId = new HashMap<>();
        fulfillmentItems.forEach(item -> byId.put(item.getOrderItemId(), item));
        List<ShippingDtos.ShipmentLineRequest> requested = lines == null || lines.isEmpty()
                ? fulfillmentItems.stream().map(item -> new ShippingDtos.ShipmentLineRequest(item.getOrderItemId(), item.getQuantity(), item.getInventoryUnitIds())).toList()
                : lines;
        List<PreparedItem> prepared = new ArrayList<>();
        Set<UUID> lineItems = new HashSet<>();
        for (ShippingDtos.ShipmentLineRequest line : requested) {
            FulfillmentItemEntity item = byId.get(line.orderItemId());
            if (item == null || !lineItems.add(line.orderItemId())) throw new ConflictException("Shipment line does not belong to this fulfillment or is duplicated");
            List<UUID> unitIds = line.inventoryUnitIds() == null ? List.of() : line.inventoryUnitIds();
            if (!item.getInventoryUnitIds().isEmpty()) {
                if (unitIds.isEmpty() || unitIds.size() != line.quantity() || !item.getInventoryUnitIds().containsAll(unitIds)) {
                    throw new ConflictException("Itemized shipment lines must contain the exact authorized InventoryUnit references");
                }
                for (UUID unitId : unitIds) prepared.add(new PreparedItem(item.getOrderItemId(), item.getSku(), item.getProductNameSnapshot(), 1, unitId, item.getReservationId()));
            } else {
                if (!unitIds.isEmpty()) throw new ConflictException("InventoryUnit references are not authorized for this non-itemized line");
                if (line.quantity() > item.getQuantity()) throw new ConflictException("Shipment quantity exceeds the fulfillment quantity");
                prepared.add(new PreparedItem(item.getOrderItemId(), item.getSku(), item.getProductNameSnapshot(), line.quantity(), null, item.getReservationId()));
            }
        }
        return prepared;
    }

    private void validateInventoryReferences(FulfillmentEntity fulfillment, List<PreparedItem> prepared) {
        Set<UUID> unitIds = new HashSet<>();
        Map<UUID, List<PreparedItem>> byReservation = new HashMap<>();
        for (PreparedItem item : prepared) {
            if (item.inventoryUnitId() != null && !unitIds.add(item.inventoryUnitId())) throw new ConflictException("An InventoryUnit is referenced more than once");
            if (item.reservationId() != null) byReservation.computeIfAbsent(item.reservationId(), ignored -> new ArrayList<>()).add(item);
        }
        if (!unitIds.isEmpty()) {
            List<ShipmentItemEntity> existing = items.findByInventoryUnitIdIn(new ArrayList<>(unitIds));
            if (!existing.isEmpty()) throw new ConflictException("An InventoryUnit is already assigned to another shipment");
        }
        for (Map.Entry<UUID, List<PreparedItem>> entry : byReservation.entrySet()) {
            InventoryClient.ReservationSnapshot reservation = inventoryClient.getReservation(entry.getKey());
            if (!Set.of("ACTIVE", "ALLOCATED", "CONFIRMED").contains(reservation.status())) throw new ConflictException("Inventory reservation is not usable");
            Map<UUID, InventoryClient.ReservedUnit> authorized = new HashMap<>();
            reservation.units().forEach(unit -> authorized.put(unit.unitId(), unit));
            for (PreparedItem item : entry.getValue()) {
                if (item.inventoryUnitId() == null) continue;
                InventoryClient.ReservedUnit unit = authorized.get(item.inventoryUnitId());
                if (unit == null || !Set.of("RESERVED", "ALLOCATED", "IN_TRANSIT", "SOLD").contains(unit.status())) {
                    throw new ConflictException("Inventory did not authorize physical unit " + item.inventoryUnitId());
                }
            }
        }
    }

    private void allocateInventory(ShipmentEntity shipment, List<PreparedItem> prepared,
                                   List<InventoryAllocation> allocations) {
        Map<UUID, List<UUID>> byReservation = new HashMap<>();
        for (PreparedItem item : prepared) {
            if (item.reservationId() != null && item.inventoryUnitId() != null) {
                byReservation.computeIfAbsent(item.reservationId(), ignored -> new ArrayList<>()).add(item.inventoryUnitId());
            }
        }
        for (Map.Entry<UUID, List<UUID>> entry : byReservation.entrySet()) {
            List<UUID> unitIds = entry.getValue().stream().distinct().toList();
            inventoryClient.shippingTransition(entry.getKey(), "ALLOCATE", shipment.getShipmentNumber(), unitIds);
            allocations.add(new InventoryAllocation(entry.getKey(), unitIds));
        }
    }

    private void releaseInventory(List<InventoryAllocation> allocations) {
        for (InventoryAllocation allocation : allocations) {
            try {
                inventoryClient.shippingTransition(allocation.reservationId(), "RELEASE", "release:" + allocation.reservationId(), allocation.unitIds());
            } catch (RuntimeException ignored) {
                // The carrier failure is already durable; reconciliation must resolve a failed release.
            }
        }
    }

    private void refreshFulfillmentAfterShipment(ShipmentEntity shipment) {
        FulfillmentEntity fulfillment = fulfillmentService.load(shipment.getFulfillmentId());
        if (isFulfillmentFullyShipped(fulfillment.getId())) {
            if (fulfillment.getStatus() == FulfillmentStatus.ALLOCATING || fulfillment.getStatus() == FulfillmentStatus.PACKED
                    || fulfillment.getStatus() == FulfillmentStatus.PARTIALLY_SHIPPED) {
                fulfillmentService.transition(fulfillment, FulfillmentStatus.SHIPPED, "FULFILLMENT_SHIPPED", "All fulfillment quantities have shipped");
            }
        } else if (fulfillment.getStatus() == FulfillmentStatus.ALLOCATING || fulfillment.getStatus() == FulfillmentStatus.PACKED) {
            fulfillmentService.transition(fulfillment, FulfillmentStatus.PARTIALLY_SHIPPED, "FULFILLMENT_PARTIALLY_SHIPPED", "A subset of fulfillment quantities has shipped");
        }
    }

    private boolean isFulfillmentFullyShipped(UUID fulfillmentId) {
        Map<UUID, Long> required = new HashMap<>();
        fulfillmentService.itemEntities(fulfillmentId).forEach(item -> required.put(item.getOrderItemId(), item.getQuantity()));
        Map<UUID, Long> shipped = new HashMap<>();
        for (ShipmentEntity shipment : shipments.findByFulfillmentIdOrderByCreatedAt(fulfillmentId)) {
            if (!Set.of(ShipmentStatus.SHIPPED, ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED, ShipmentStatus.RETURNED).contains(shipment.getStatus())) continue;
            for (ShipmentItemEntity item : items.findByShipment_IdOrderByCreatedAt(shipment.getId())) {
                shipped.merge(item.getOrderItemId(), item.getQuantity(), Long::sum);
            }
        }
        return required.entrySet().stream().allMatch(entry -> shipped.getOrDefault(entry.getKey(), 0L) >= entry.getValue());
    }

    private void notifyOrder(ShipmentEntity shipment, String status) {
        try {
            orderClient.notifyShipment(shipment.getOrderId(), shipment.getShipmentNumber(), status);
            persistence.markOrderNotificationSuccess(shipment.getId());
        } catch (RuntimeException ex) {
            persistence.markOrderNotificationFailure(shipment.getId(), "Order notification failed: " + ex.getMessage());
        }
    }

    private ShippingDtos.ShipmentResponse response(ShipmentEntity shipment, boolean operational) {
        List<ShippingDtos.ShipmentItemResponse> itemResponses = items.findByShipment_IdOrderByCreatedAt(shipment.getId()).stream()
                .map(item -> operational ? ShippingDtos.ShipmentItemResponse.from(item)
                        : new ShippingDtos.ShipmentItemResponse(item.getId(), shipment.getId(), item.getOrderItemId(), item.getSku(),
                        item.getProductNameSnapshot(), item.getQuantity(), null, item.getCreatedAt())).toList();
        return new ShippingDtos.ShipmentResponse(shipment.getId(), shipment.getShipmentNumber(), shipment.getFulfillmentId(),
                shipment.getOrderId(), shipment.getOrderNumber(), shipment.getCustomerId(), shipment.getStatus(), shipment.getCarrier(),
                shipment.getServiceLevel(), shipment.getTrackingNumber(), operational ? shipment.getProviderShipmentId() : null,
                operational ? shipment.getLabelReference() : null, shipment.getShippingCost(), shipment.getCurrency(), shipment.getPackageCount(),
                shipment.getEstimatedDeliveryAt(), shipment.isOrderNotificationPending(), shipment.getCreatedAt(), shipment.getUpdatedAt(),
                shipment.getShippedAt(), shipment.getDeliveredAt(), shipment.getCancelledAt(), itemResponses,
                history.findByShipment_IdOrderByCreatedAtAsc(shipment.getId()).stream().map(ShippingDtos.ShipmentHistoryResponse::from).toList());
    }

    private void assertVisible(ShipmentEntity shipment, Authentication authentication) {
        String subject = SecurityAccess.subject(authentication);
        if (!SecurityAccess.has(authentication, "SHIPPING_READ") && !subject.equals(shipment.getCustomerId())) {
            throw new NotFoundException("Shipment not found: " + shipment.getId());
        }
    }

    private PageRequest pageable(int page, int size, String rawSort) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("page must be >= 0 and size must be between 1 and 100");
        String[] parts = (rawSort == null || rawSort.isBlank() ? "createdAt,desc" : rawSort.trim()).split(",");
        if (parts.length > 2 || !List.of("createdAt", "updatedAt", "status", "shipmentNumber", "orderNumber").contains(parts[0].trim())) {
            throw new BadRequestException("Unsupported shipment sort; use createdAt,desc");
        }
        Sort.Direction direction = parts.length == 2 ? Sort.Direction.fromOptionalString(parts[1].trim()).orElse(null) : Sort.Direction.DESC;
        if (direction == null) throw new BadRequestException("Sort direction must be asc or desc");
        return PageRequest.of(page, size, Sort.by(direction, parts[0].trim()));
    }

    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    public record PreparedItem(UUID orderItemId, String sku, String productNameSnapshot, long quantity,
                               UUID inventoryUnitId, UUID reservationId) { }

    private record InventoryAllocation(UUID reservationId, List<UUID> unitIds) { }
}
