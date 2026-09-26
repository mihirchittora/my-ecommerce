package com.shop.order.service;

import com.shop.order.api.OrderDtos;
import com.shop.order.client.CatalogClient;
import com.shop.order.client.CatalogSku;
import com.shop.order.client.InventoryClient;
import com.shop.order.client.InventoryReservation;
import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderEventType;
import com.shop.order.domain.OrderHistory;
import com.shop.order.domain.OrderIdempotency;
import com.shop.order.domain.OrderItemStatus;
import com.shop.order.domain.OrderRepository;
import com.shop.order.domain.OrderShippingAddress;
import com.shop.order.domain.OrderStatus;
import com.shop.order.domain.PaymentMethod;
import com.shop.order.exception.BadRequestException;
import com.shop.order.exception.ConflictException;
import com.shop.order.exception.InsufficientInventoryException;
import com.shop.order.exception.MalformedDependencyResponseException;
import com.shop.order.exception.NotFoundException;
import com.shop.order.exception.OrderApiException;
import com.shop.order.exception.RemoteDependencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderApplicationService {
    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);
    private final CatalogClient catalog;
    private final InventoryClient inventory;
    private final OrderRepository orders;
    private final OrderNumberGenerator orderNumbers;
    private final OrderIdempotencyService idempotency;
    private final OrderWriteService writes;
    private final OrderReadService reads;
    private final PaymentMethodEligibilityService paymentMethods;
    private final FulfillmentOrchestrator fulfillmentOrchestrator;
    private final ShippingChargeCalculator shippingCharges;
    private final TaxCalculator taxCalculator;
    private final CouponService coupons;
    private final long reservationExpiryMinutes;

    public OrderApplicationService(CatalogClient catalog,
                                   InventoryClient inventory,
                                   OrderRepository orders,
                                   OrderNumberGenerator orderNumbers,
                                   OrderIdempotencyService idempotency,
                                   OrderWriteService writes,
                                   OrderReadService reads,
                                   PaymentMethodEligibilityService paymentMethods,
                                   FulfillmentOrchestrator fulfillmentOrchestrator,
                                   ShippingChargeCalculator shippingCharges,
                                   TaxCalculator taxCalculator,
                                   CouponService coupons,
                                   @Value("${app.reservation.expiry-minutes:15}") long reservationExpiryMinutes) {
        this.catalog = catalog;
        this.inventory = inventory;
        this.orders = orders;
        this.orderNumbers = orderNumbers;
        this.idempotency = idempotency;
        this.writes = writes;
        this.reads = reads;
        this.paymentMethods = paymentMethods;
        this.fulfillmentOrchestrator = fulfillmentOrchestrator;
        this.shippingCharges = shippingCharges;
        this.taxCalculator = taxCalculator;
        this.coupons = coupons;
        this.reservationExpiryMinutes = reservationExpiryMinutes;
    }

    public OrderDtos.OrderResponse create(OrderDtos.CreateOrderRequest request, String idempotencyKey,
                                          Authentication authentication) {
        String customerId = subject(authentication);
        String key = normalizeIdempotencyKey(idempotencyKey);
        OrderRequestNormalizer.NormalizedRequest normalized = OrderRequestNormalizer.normalize(request);
        validateCurrency(normalized.currency());
        String requestHash = IdempotencyFingerprint.sha256(normalized);

        OrderIdempotencyService.ClaimResult claimResult = idempotency.claim(customerId, key, requestHash);
        OrderIdempotency claim = claimResult.claim();
        if (!claimResult.newlyCreated()) {
            if (!requestHash.equals(claim.getRequestHash())) {
                throw new ConflictException("Idempotency-Key was already used with a different request payload");
            }
            UUID existingId = claim.getOrderId();
            if (existingId == null) {
                existingId = orders.findByCustomerIdAndIdempotencyKey(customerId, key)
                        .map(CustomerOrder::getId).orElse(null);
            }
            if (existingId == null) {
                throw new ConflictException("An order with this Idempotency-Key is still in progress");
            }
            CustomerOrder existing = reads.detailed(existingId);
            if (existing.getStatus() == OrderStatus.PENDING_RESERVATION) {
                continueCheckout(existing.getId(), normalized.preferredLocationId(), customerId);
            }
            return reads.response(existingId, hasPermission(authentication, "ORDER_READ"));
        }

        List<CatalogSku> snapshots;
        try {
            snapshots = normalized.lines().stream().map(line -> catalog.getSellableSku(line.sku())).toList();
            validateCatalogCurrency(normalized.currency(), snapshots);
        } catch (OrderApiException ex) {
            idempotency.delete(claim);
            log.warn("Catalog lookup failed while preparing customer order customerId={}", customerId);
            throw ex;
        }

        CustomerOrder order = null;
        try {
            order = buildPendingOrder(customerId, key, requestHash, normalized, snapshots);
            paymentMethods.requireEligible(order.getPaymentMethod(), order.getCurrency(), order.getTotalAmount(),
                    order.getShippingAddress().getCountry());
            writes.createPending(order);
            idempotency.linkToOrder(claim, order.getId());
        } catch (RuntimeException ex) {
            if (order != null && order.getId() != null) coupons.releaseForOrder(order.getId());
            idempotency.delete(claim);
            throw ex;
        }

        log.info("Order created orderId={} orderNumber={} customerId={} status={}",
                order.getId(), order.getOrderNumber(), customerId, order.getStatus());
        continueCheckout(order.getId(), normalized.preferredLocationId(), customerId);
        return reads.response(order.getId(), hasPermission(authentication, "ORDER_READ"));
    }

    public Page<OrderDtos.OrderSummaryResponse> listMy(int page, int size, String sort, Authentication authentication) {
        return reads.myOrders(subject(authentication), pageable(page, size, sort));
    }

    public OrderDtos.PaymentMethodOptionsResponse paymentMethods(String currency, BigDecimal amount, String country,
                                                                 Authentication authentication) {
        subject(authentication);
        return paymentMethods.options(currency, amount, country);
    }

    public OrderDtos.CouponPreviewResponse previewCoupon(OrderDtos.CouponPreviewRequest request,
                                                         Authentication authentication) {
        String customerId = subject(authentication);
        CouponService.CouponResult result = coupons.preview(request.couponCode(), customerId, request.subtotal());
        return new OrderDtos.CouponPreviewResponse(result.code(), result.discount(), result.type(),
                result.value(), result.maximumDiscount());
    }

    public OrderDtos.ShippingPreviewResponse shippingPreview(OrderDtos.ShippingPreviewRequest request,
                                                              Authentication authentication) {
        String customerId = subject(authentication);
        BigDecimal subtotal = request.subtotal().setScale(2, RoundingMode.HALF_UP);
        CouponService.CouponResult coupon = request.couponCode() == null || request.couponCode().isBlank()
                ? CouponService.CouponResult.none()
                : coupons.preview(request.couponCode(), customerId, subtotal);
        BigDecimal merchandiseAmount = subtotal.subtract(coupon.discount()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        List<OrderDtos.ShippingOption> options = List.of(
                new OrderDtos.ShippingOption("STANDARD", shippingCharges.calculate(merchandiseAmount, request.country(), "STANDARD")),
                new OrderDtos.ShippingOption("EXPRESS", shippingCharges.calculate(merchandiseAmount, request.country(), "EXPRESS")));
        return new OrderDtos.ShippingPreviewResponse(subtotal, coupon.discount(), merchandiseAmount, options);
    }

    public Page<OrderDtos.OrderSummaryResponse> list(OrderStatus status, String orderNumber, String customerId,
                                                     String sku, Instant createdFrom, Instant createdTo,
                                                     int page, int size, String sort, Authentication authentication) {
        requirePermission(authentication, "ORDER_READ");
        if (createdFrom != null && createdTo != null && !createdFrom.isBefore(createdTo)) {
            throw new BadRequestException("createdFrom must be before createdTo");
        }
        return reads.search(status, blankToNull(orderNumber), blankToNull(customerId), normalizeOptionalSku(sku),
                createdFrom, createdTo, pageable(page, size, sort));
    }

    public OrderDtos.OrderResponse get(UUID orderId, Authentication authentication) {
        CustomerOrder order = reads.detailed(orderId);
        boolean operational = hasPermission(authentication, "ORDER_READ");
        if (!operational && !order.getCustomerId().equals(subject(authentication))) {
            throw new NotFoundException("Order not found: " + orderId);
        }
        return OrderDtos.response(order, operational);
    }

    public OrderDtos.OrderResponse cancel(UUID orderId, Authentication authentication) {
        String actor = subject(authentication);
        CustomerOrder order = reads.detailed(orderId);
        boolean owner = order.getCustomerId().equals(actor);
        if (!owner && !hasPermission(authentication, "ORDER_CANCEL")) {
            throw new NotFoundException("Order not found: " + orderId);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return OrderDtos.response(order, hasPermission(authentication, "ORDER_READ"));
        }
        if (!OrderStateMachine.cancellable(order.getStatus())) {
            throw new ConflictException("Order cannot be cancelled after it reaches " + order.getStatus());
        }
        for (var item : order.getItems()) {
            if (item.getReservationId() != null) inventory.release(item.getReservationId());
        }
        writes.cancel(orderId, actor);
        coupons.releaseForOrder(orderId);
        log.info("Order cancelled orderId={} orderNumber={} customerId={}", orderId, order.getOrderNumber(), actor);
        return reads.response(orderId, hasPermission(authentication, "ORDER_READ"));
    }

    public OrderDtos.OrderResponse cancelItem(UUID orderId, UUID itemId, Authentication authentication) {
        String actor = subject(authentication);
        CustomerOrder order = reads.detailed(orderId);
        boolean owner = order.getCustomerId().equals(actor);
        if (!owner && !hasPermission(authentication, "ORDER_CANCEL")) {
            throw new NotFoundException("Order not found: " + orderId);
        }
        var item = order.getItems().stream().filter(candidate -> candidate.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Order item not found: " + itemId));
        if (item.getStatus() == OrderItemStatus.CANCELLED) {
            return OrderDtos.response(order, hasPermission(authentication, "ORDER_READ"));
        }
        if (!OrderStateMachine.itemCancellable(order.getStatus())) {
            throw new ConflictException("This item cannot be cancelled after the order is shipped");
        }
        if (item.getReservationId() != null) inventory.release(item.getReservationId());
        writes.cancelItem(orderId, itemId, actor);
        CustomerOrder updated = reads.detailed(orderId);
        if (updated.getStatus() == OrderStatus.CANCELLED) coupons.releaseForOrder(orderId);
        return OrderDtos.response(updated, hasPermission(authentication, "ORDER_READ"));
    }

    private void continueCheckout(UUID orderId, UUID preferredLocationId, String actorUserId) {
        CustomerOrder order = reads.detailed(orderId);
        if (order.getStatus() != OrderStatus.PENDING_RESERVATION) return;
        List<ItemReservation> successful = new ArrayList<>();
        try {
            for (var item : order.getItems()) {
                if (item.getStatus() == OrderItemStatus.CANCELLED) continue;
                if (item.getReservationId() != null) continue;
                String reference = item.getReservationReference();
                Instant expiresAt = Instant.now().plusSeconds(reservationExpiryMinutes * 60);
                InventoryReservation reservation = inventory.reserve(item.getSku(), preferredLocationId,
                        item.getQuantity(), reference, expiresAt);
                if (reservation == null) {
                    throw new MalformedDependencyResponseException("Inventory", "reservation response is empty");
                }
                successful.add(new ItemReservation(item.getId(), reservation));
                writes.attachReservation(orderId, item.getId(), reservation);
                log.info("Inventory reservation succeeded orderId={} orderNumber={} sku={} reservationId={}",
                        orderId, order.getOrderNumber(), item.getSku(), reservation.reservationId());
            }
            writes.markReservedAndPendingPayment(orderId, actorUserId, order.getPaymentMethod());
            if (order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) {
                fulfillmentOrchestrator.enqueue(orderId);
            }
        } catch (InsufficientInventoryException ex) {
            boolean compensated = compensate(orderId, successful);
            if (compensated) {
                coupons.releaseForOrder(orderId);
                writes.markFailed(orderId, ex.getMessage(), actorUserId);
            }
            else writes.recordReservationFailure(orderId, "Inventory was insufficient and compensation requires recovery", actorUserId);
            log.warn("Inventory reservation failed orderId={} orderNumber={}", orderId, order.getOrderNumber());
            throw compensated ? ex : new RemoteDependencyException("Inventory", "could not safely compensate a partial reservation", ex);
        } catch (RemoteDependencyException ex) {
            boolean compensated = compensate(orderId, successful);
            writes.recordReservationFailure(orderId, ex.getMessage(), actorUserId);
            if (!compensated) {
                throw new RemoteDependencyException("Inventory", "reservation failed and compensation requires recovery", ex);
            }
            throw ex;
        } catch (RuntimeException ex) {
            compensate(orderId, successful);
            writes.recordReservationFailure(orderId, "Unexpected reservation failure", actorUserId);
            throw ex;
        }
    }

    private boolean compensate(UUID orderId, List<ItemReservation> successful) {
        boolean allReleased = true;
        for (ItemReservation itemReservation : successful) {
            try {
                inventory.release(itemReservation.reservation().reservationId());
                writes.clearReservation(orderId, itemReservation.itemId());
            } catch (RuntimeException ex) {
                allReleased = false;
                log.error("Inventory compensation failed orderId={} reservationId={}", orderId,
                        itemReservation.reservation().reservationId(), ex);
            }
        }
        return allReleased;
    }

    private CustomerOrder buildPendingOrder(String customerId, String idempotencyKey, String requestHash,
                                            OrderRequestNormalizer.NormalizedRequest request,
                                            List<CatalogSku> snapshots) {
        Map<String, CatalogSku> bySku = new LinkedHashMap<>();
        snapshots.forEach(snapshot -> bySku.put(snapshot.sku().trim().toUpperCase(Locale.ROOT), snapshot));
        List<BigDecimal> subtotals = new ArrayList<>();
        List<TaxCalculator.TaxLine> taxLines = new ArrayList<>();
        CustomerOrder order = new CustomerOrder();
        // Coupon redemptions reference the order before it is persisted. Keep the
        // version nullable so Spring Data still treats this generated-id entity as new.
        order.setId(UUID.randomUUID());
        order.setOrderNumber(orderNumbers.next());
        order.setCustomerId(customerId);
        order.setIdempotencyKey(idempotencyKey);
        order.setIdempotencyPayloadHash(requestHash);
        order.setStatus(OrderStatus.PENDING_RESERVATION);
        order.setPaymentMethod(request.paymentMethod());
        order.setCurrency(request.currency());
        order.attachShippingAddress(OrderShippingAddress.from(request.shippingAddress()));

        for (OrderRequestNormalizer.NormalizedLine line : request.lines()) {
            CatalogSku snapshot = bySku.get(line.sku());
            BigDecimal subtotal = OrderPricing.lineSubtotal(snapshot.price(), line.quantity());
            subtotals.add(subtotal);
            taxLines.add(new TaxCalculator.TaxLine(subtotal, snapshot.taxRate()));
            var item = new com.shop.order.domain.OrderItem();
            item.setSku(line.sku());
            item.setProductNameSnapshot(snapshot.productName());
            Map<String, Object> variantSnapshot = new LinkedHashMap<>();
            if (snapshot.variantName() != null && !snapshot.variantName().isBlank()) {
                variantSnapshot.put("name", snapshot.variantName());
            }
            if (snapshot.productId() != null) variantSnapshot.put("productId", snapshot.productId().toString());
            if (snapshot.variantId() != null) variantSnapshot.put("variantId", snapshot.variantId().toString());
            variantSnapshot.put("attributes", snapshot.attributes());
            variantSnapshot.put("taxRate", snapshot.taxRate());
            variantSnapshot.put("priceIncludingTax", snapshot.price().add(snapshot.price()
                    .multiply(snapshot.taxRate() == null ? BigDecimal.ZERO : snapshot.taxRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP));
            item.setVariantSnapshot(variantSnapshot);
            item.setUnitPrice(snapshot.price().setScale(2));
            item.setCurrency(snapshot.currency().trim().toUpperCase(Locale.ROOT));
            item.setQuantity(line.quantity());
            item.setSubtotal(subtotal);
            item.setReservationReference(reservationReference(order.getOrderNumber(), line.sku(), request.lines().size()));
            order.addItem(item);
        }

        BigDecimal subtotal = subtotals.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        CouponService.CouponResult coupon = coupons.reserve(request.couponCode(), customerId, order.getId(), subtotal);
        BigDecimal merchandiseAfterDiscount = subtotal.subtract(coupon.discount()).max(BigDecimal.ZERO);
        BigDecimal shipping = shippingCharges.calculate(merchandiseAfterDiscount, request.shippingAddress().country(), request.serviceLevel());
        TaxCalculator.TaxBreakdown tax = taxCalculator.calculate(taxLines, coupon.discount(), shipping,
                request.currency(), request.shippingAddress().country());
        OrderPricing.Totals totals = OrderPricing.calculate(subtotal, coupon.discount(), shipping, tax.taxAmount());
        order.setSubtotal(totals.subtotal());
        order.setDiscountAmount(totals.discountAmount());
        order.setShippingAmount(totals.shippingAmount());
        order.setTaxAmount(totals.taxAmount());
        order.setTaxableAmount(tax.taxableAmount());
        order.setTaxRate(tax.taxRate());
        order.setCouponCode(coupon.code());
        order.setTotalAmount(totals.totalAmount());
        allocateFinancialSnapshots(order, coupon.discount(), tax);
        addHistory(order, null, OrderStatus.PENDING_RESERVATION, OrderEventType.ORDER_CREATED,
                order.getOrderNumber(), "Order created from a catalog price snapshot", customerId);
        addHistory(order, OrderStatus.PENDING_RESERVATION, OrderStatus.PENDING_RESERVATION,
                OrderEventType.RESERVATION_REQUESTED, order.getOrderNumber(), "Inventory reservation requested", customerId);
        return order;
    }

    private void allocateFinancialSnapshots(CustomerOrder order, BigDecimal discount, TaxCalculator.TaxBreakdown tax) {
        BigDecimal subtotal = order.getSubtotal();
        for (int index = 0; index < order.getItems().size(); index++) {
            var item = order.getItems().get(index);
            BigDecimal share = subtotal.signum() == 0 ? BigDecimal.ZERO : item.getSubtotal().divide(subtotal, 8, java.math.RoundingMode.HALF_UP);
            BigDecimal itemDiscount = discount.multiply(share).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal itemTaxable = item.getSubtotal().subtract(itemDiscount).max(BigDecimal.ZERO);
            BigDecimal merchandiseTax = tax.lineTaxAmounts().size() > index
                    ? tax.lineTaxAmounts().get(index) : BigDecimal.ZERO;
            BigDecimal itemTax = merchandiseTax.add(tax.shippingTaxAmount().multiply(share))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            item.setDiscountAmount(itemDiscount);
            item.setTaxableAmount(itemTaxable);
            item.setTaxAmount(itemTax);
        }
    }

    private String reservationReference(String orderNumber, String sku, int itemCount) {
        return itemCount == 1 ? orderNumber : orderNumber + ":" + sku;
    }

    private void validateCatalogCurrency(String requestedCurrency, List<CatalogSku> snapshots) {
        for (CatalogSku snapshot : snapshots) {
            if (!requestedCurrency.equals(snapshot.currency().trim().toUpperCase(Locale.ROOT))) {
                throw new BadRequestException("Order currency must match the Catalog currency for every SKU");
            }
        }
    }

    private void validateCurrency(String currency) {
        try {
            com.shop.order.domain.SupportedCurrency.valueOf(currency);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported currency: " + currency);
        }
    }

    private PageRequest pageable(int page, int size, String rawSort) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("page must be >= 0 and size must be between 1 and 100");
        String sort = rawSort == null || rawSort.isBlank() ? "createdAt,desc" : rawSort.trim();
        String[] parts = sort.split(",");
        if (parts.length > 2) throw new BadRequestException("sort must be property,direction");
        String property = parts[0].trim();
        if (!List.of("createdAt", "updatedAt", "orderNumber", "totalAmount", "status", "customerId").contains(property)) {
            throw new BadRequestException("Invalid sort property: " + property);
        }
        Sort.Direction direction = parts.length == 2 ? Sort.Direction.fromOptionalString(parts[1].trim()).orElse(null) : Sort.Direction.DESC;
        if (direction == null) throw new BadRequestException("Invalid sort direction: " + parts[1]);
        return PageRequest.of(page, size, Sort.by(direction, property));
    }

    private String subject(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            throw new com.shop.order.exception.OrderApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED", "Authentication is required");
        }
        return authentication.getName();
    }

    private void requirePermission(Authentication authentication, String permission) {
        if (!hasPermission(authentication, permission)) {
            throw new com.shop.order.exception.OrderApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "FORBIDDEN", "Insufficient permission");
        }
    }

    private boolean hasPermission(Authentication authentication, String permission) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> permission.equals(authority.getAuthority()));
    }

    private String normalizeIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 200) {
            throw new BadRequestException("Idempotency-Key is required and must be at most 200 characters");
        }
        return key.trim();
    }

    private String normalizeOptionalSku(String sku) {
        return sku == null || sku.isBlank() ? null : sku.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void addHistory(CustomerOrder order, OrderStatus from, OrderStatus to, OrderEventType eventType,
                             String referenceId, String notes, String actorUserId) {
        OrderHistory history = new OrderHistory();
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setEventType(eventType);
        history.setReferenceId(referenceId);
        history.setNotes(notes);
        history.setActorUserId(actorUserId);
        order.addHistory(history);
    }

    private record ItemReservation(UUID itemId, InventoryReservation reservation) {
    }

}
