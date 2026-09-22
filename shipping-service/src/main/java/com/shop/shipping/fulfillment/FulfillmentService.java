package com.shop.shipping.fulfillment;

import com.shop.shipping.api.ShippingDtos;
import com.shop.shipping.common.BadRequestException;
import com.shop.shipping.common.ConflictException;
import com.shop.shipping.common.NotFoundException;
import com.shop.shipping.order.OrderClient;
import com.shop.shipping.security.SecurityAccess;
import com.shop.shipping.shipment.ShipmentEntity;
import com.shop.shipping.shipment.ShipmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FulfillmentService {
    private final FulfillmentRepository fulfillments;
    private final FulfillmentItemRepository items;
    private final FulfillmentHistoryRepository history;
    private final ShipmentRepository shipments;
    private final OrderClient orderClient;

    public FulfillmentService(FulfillmentRepository fulfillments, FulfillmentItemRepository items,
                              FulfillmentHistoryRepository history, ShipmentRepository shipments,
                              OrderClient orderClient) {
        this.fulfillments = fulfillments;
        this.items = items;
        this.history = history;
        this.shipments = shipments;
        this.orderClient = orderClient;
    }

    public FulfillmentEntity create(ShippingDtos.CreateFulfillmentRequest request) {
        FulfillmentEntity existing = fulfillments.findByOrderId(request.orderId()).orElse(null);
        if (existing != null) return existing;
        OrderClient.OrderSnapshot order = orderClient.getOrder(request.orderId());
        if (!SetOfShippable.ORDER.contains(order.status())) {
            throw new ConflictException("Order must be CONFIRMED or FULFILLING before fulfillment creation");
        }
        FulfillmentEntity fulfillment = new FulfillmentEntity();
        fulfillment.setOrderId(order.id());
        fulfillment.setOrderNumber(order.orderNumber());
        fulfillment.setCustomerId(order.customerId());
        fulfillment.setShippingAddressReference(order.shippingAddress().sourceAddressId() == null
                ? null : order.shippingAddress().sourceAddressId().toString());
        fulfillment.setShippingRecipientName(order.shippingAddress().recipientName());
        fulfillment.setShippingPhone(order.shippingAddress().phone());
        fulfillment.setShippingLine1(order.shippingAddress().line1());
        fulfillment.setShippingLine2(order.shippingAddress().line2());
        fulfillment.setShippingCity(order.shippingAddress().city());
        fulfillment.setShippingState(order.shippingAddress().state());
        fulfillment.setShippingPostalCode(order.shippingAddress().postalCode());
        fulfillment.setShippingCountry(order.shippingAddress().country());
        fulfillment.setShippingLandmark(order.shippingAddress().landmark());
        fulfillment.setStatus(FulfillmentStatus.PENDING);
        fulfillment = fulfillments.saveAndFlush(fulfillment);
        for (OrderClient.OrderItemSnapshot source : order.items()) {
            if (source.id() == null || source.sku() == null || source.quantity() < 1) {
                throw new ConflictException("Order contains an incomplete item");
            }
            FulfillmentItemEntity item = new FulfillmentItemEntity();
            item.setFulfillment(fulfillment);
            item.setOrderItemId(source.id());
            item.setSku(source.sku());
            item.setProductNameSnapshot(source.productNameSnapshot());
            item.setQuantity(source.quantity());
            item.setReservationId(source.reservationId());
            if (source.inventoryUnits() != null) {
                item.setInventoryUnitIds(source.inventoryUnits().stream().map(OrderClient.InventoryUnitReference::id).toList());
                item.setInventoryUnitCodes(source.inventoryUnits().stream().map(OrderClient.InventoryUnitReference::unitCode).toList());
            }
            items.save(item);
        }
        addHistory(fulfillment, null, FulfillmentStatus.PENDING, "FULFILLMENT_CREATED",
                fulfillment.getOrderNumber(), "Fulfillment created from the Order snapshot");
        transition(fulfillment, FulfillmentStatus.READY, "FULFILLMENT_READY", "Order is eligible for fulfillment");
        return fulfillment;
    }

    public ShippingDtos.FulfillmentResponse get(UUID id, Authentication authentication) {
        FulfillmentEntity fulfillment = load(id);
        assertVisible(fulfillment, authentication);
        return response(fulfillment);
    }

    public ShippingDtos.FulfillmentResponse getByOrder(UUID orderId, Authentication authentication) {
        FulfillmentEntity fulfillment = fulfillments.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Fulfillment not found for Order: " + orderId));
        assertVisible(fulfillment, authentication);
        return response(fulfillment);
    }

    public Page<ShippingDtos.FulfillmentResponse> list(int page, int size, String sort, Authentication authentication) {
        SecurityAccess.require(authentication, "SHIPPING_READ");
        return fulfillments.findAll(pageable(page, size, sort)).map(this::response);
    }

    public FulfillmentEntity load(UUID id) {
        return fulfillments.findById(id).orElseThrow(() -> new NotFoundException("Fulfillment not found: " + id));
    }

    public List<FulfillmentItemEntity> itemEntities(UUID id) { return items.findByFulfillment_IdOrderByCreatedAt(id); }
    public List<ShipmentEntity> shipmentEntities(UUID id) { return shipments.findByFulfillmentIdOrderByCreatedAt(id); }
    public List<FulfillmentHistoryEntity> history(UUID id) { return history.findByFulfillment_IdOrderByCreatedAtAsc(id); }

    public void transition(FulfillmentEntity fulfillment, FulfillmentStatus target, String eventType, String notes) {
        FulfillmentStateMachine.requireTransition(fulfillment.getStatus(), target);
        if (fulfillment.getStatus() == target) return;
        FulfillmentStatus from = fulfillment.getStatus();
        fulfillment.setStatus(target);
        if (target == FulfillmentStatus.COMPLETED || target == FulfillmentStatus.DELIVERED) fulfillment.setCompletedAt(java.time.Instant.now());
        if (target == FulfillmentStatus.CANCELLED) fulfillment.setCancelledAt(java.time.Instant.now());
        fulfillments.saveAndFlush(fulfillment);
        addHistory(fulfillment, from, target, eventType, fulfillment.getOrderNumber(), notes);
    }

    private void addHistory(FulfillmentEntity fulfillment, FulfillmentStatus from, FulfillmentStatus to,
                            String eventType, String referenceId, String notes) {
        FulfillmentHistoryEntity event = new FulfillmentHistoryEntity();
        event.setFulfillment(fulfillment);
        event.setFromStatus(from);
        event.setToStatus(to);
        event.setEventType(eventType);
        event.setReferenceId(referenceId);
        event.setNotes(notes);
        history.save(event);
    }

    private void assertVisible(FulfillmentEntity fulfillment, Authentication authentication) {
        String subject = SecurityAccess.subject(authentication);
        if (!SecurityAccess.has(authentication, "SHIPPING_READ") && !subject.equals(fulfillment.getCustomerId())) {
            throw new NotFoundException("Fulfillment not found: " + fulfillment.getId());
        }
    }

    public ShippingDtos.FulfillmentResponse response(FulfillmentEntity fulfillment) {
        List<FulfillmentItemEntity> fulfillmentItems = itemEntities(fulfillment.getId());
        List<FulfillmentHistoryEntity> fulfillmentHistory = history(fulfillment.getId());
        UUID sourceAddressId = parseSourceAddressId(fulfillment.getShippingAddressReference());
        return new ShippingDtos.FulfillmentResponse(fulfillment.getId(), fulfillment.getOrderId(), fulfillment.getOrderNumber(),
                fulfillment.getCustomerId(), fulfillment.getStatus(), fulfillment.getShippingAddressReference(),
                new ShippingDtos.ShippingAddressResponse(
                        sourceAddressId,
                        fulfillment.getShippingRecipientName(), fulfillment.getShippingPhone(), fulfillment.getShippingLine1(),
                        fulfillment.getShippingLine2(), fulfillment.getShippingCity(), fulfillment.getShippingState(),
                        fulfillment.getShippingPostalCode(), fulfillment.getShippingCountry(), fulfillment.getShippingLandmark()),
                fulfillment.getCreatedAt(), fulfillment.getUpdatedAt(), fulfillment.getCompletedAt(), fulfillment.getCancelledAt(),
                fulfillmentItems.stream().map(ShippingDtos.FulfillmentItemResponse::from).toList(),
                fulfillmentHistory.stream().map(ShippingDtos.FulfillmentHistoryResponse::from).toList(),
                shipmentEntities(fulfillment.getId()).stream().map(ShippingDtos.ShipmentSummaryResponse::from).toList());
    }

    private UUID parseSourceAddressId(String reference) {
        if (reference == null || reference.isBlank()) return null;
        try {
            return UUID.fromString(reference);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private PageRequest pageable(int page, int size, String rawSort) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("page must be >= 0 and size must be between 1 and 100");
        String[] parts = (rawSort == null || rawSort.isBlank() ? "createdAt,desc" : rawSort.trim()).split(",");
        if (parts.length > 2 || !List.of("createdAt", "updatedAt", "status", "orderNumber").contains(parts[0].trim())) {
            throw new BadRequestException("Unsupported fulfillment sort; use createdAt,desc");
        }
        Sort.Direction direction = parts.length == 2 ? Sort.Direction.fromOptionalString(parts[1].trim()).orElse(null) : Sort.Direction.DESC;
        if (direction == null) throw new BadRequestException("Sort direction must be asc or desc");
        return PageRequest.of(page, size, Sort.by(direction, parts[0].trim()));
    }

    private static final class SetOfShippable {
        private static final List<String> ORDER = List.of("CONFIRMED", "FULFILLING");
        private SetOfShippable() { }
    }
}
