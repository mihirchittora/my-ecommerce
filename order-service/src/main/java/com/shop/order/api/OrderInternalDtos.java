package com.shop.order.api;

import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderItem;
import com.shop.order.domain.OrderItemInventoryUnit;
import com.shop.order.domain.OrderItemStatus;
import com.shop.order.domain.PaymentMethod;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class OrderInternalDtos {
    private OrderInternalDtos() { }

    public record OrderResponse(UUID id, String orderNumber, String customerId, String status, String currency,
                                PaymentMethod paymentMethod, List<ItemResponse> items, ShippingAddressResponse shippingAddress) {
        public static OrderResponse from(CustomerOrder order) {
            return new OrderResponse(order.getId(), order.getOrderNumber(), order.getCustomerId(), order.getStatus().name(),
                    order.getCurrency(), order.getPaymentMethod(), order.getItems().stream()
                            .filter(item -> item.getStatus() == OrderItemStatus.ACTIVE)
                            .map(ItemResponse::from).toList(),
                    ShippingAddressResponse.from(order.getShippingAddress()));
        }
    }

    public record ItemResponse(UUID id, String sku, String productNameSnapshot, long quantity,
                               UUID reservationId, String reservationReference, List<UnitReference> inventoryUnits) {
        static ItemResponse from(OrderItem item) {
            return new ItemResponse(item.getId(), item.getSku(), item.getProductNameSnapshot(), item.getQuantity(),
                    item.getReservationId(), item.getReservationReference(), item.getInventoryUnitReferences().stream()
                    .map(reference -> new UnitReference(reference.getInventoryUnitId(), reference.getUnitCode())).toList());
        }
    }

    public record UnitReference(UUID id, String unitCode) { }

    public record ShippingAddressResponse(UUID sourceAddressId, String recipientName, String phone, String line1,
                                          String line2, String city, String state, String postalCode,
                                          String country, String landmark) {
        static ShippingAddressResponse from(com.shop.order.domain.OrderShippingAddress address) {
            return address == null ? null : new ShippingAddressResponse(address.getSourceAddressId(),
                    address.getRecipientName(), address.getPhone(), address.getLine1(), address.getLine2(),
                    address.getCity(), address.getState(), address.getPostalCode(), address.getCountry(),
                    address.getLandmark());
        }
    }
    public record ShippingEventRequest(@NotBlank @Size(max = 40) String shipmentNumber,
                                       @NotBlank @Size(max = 30) String status) { }

    public record PaymentEventRequest(@NotBlank @Size(max = 80) String paymentId,
                                      @NotBlank @Size(max = 80) String customerId,
                                      @NotBlank @Size(max = 30) String paymentStatus,
                                      BigDecimal amount,
                                      @Size(min = 3, max = 3) String currency,
                                      @Size(max = 40) String provider,
                                      @Size(max = 200) String providerPaymentId,
                                      PaymentMethod paymentMethod) {
        public PaymentEventRequest(String paymentId, String customerId, String paymentStatus, BigDecimal amount,
                                   String currency, String provider, String providerPaymentId) {
            this(paymentId, customerId, paymentStatus, amount, currency, provider, providerPaymentId, PaymentMethod.ONLINE);
        }
    }

    public record PaymentValidationResponse(UUID id, String orderNumber, String customerId, String status,
                                            String currency, java.math.BigDecimal totalAmount, PaymentMethod paymentMethod) {
        public static PaymentValidationResponse from(CustomerOrder order) {
            return new PaymentValidationResponse(order.getId(), order.getOrderNumber(), order.getCustomerId(),
                    order.getStatus().name(), order.getCurrency(), order.getTotalAmount(), order.getPaymentMethod());
        }
    }
}
