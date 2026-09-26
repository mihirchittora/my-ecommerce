package com.shop.order.returns;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReturnDtos {
    private ReturnDtos() { }
    public record ItemRequest(@NotNull UUID orderItemId, @Min(1) long quantity, @NotNull ReturnReason reason, @Size(max = 500) String resolution) { }
    public record CreateRequest(@NotNull UUID orderId, @NotEmpty List<@Valid ItemRequest> items, @Size(max = 1000) String comment) { }
    public record ItemResponse(UUID id, UUID orderItemId, String sku, long quantity, ReturnReason reason, String resolution) { static ItemResponse from(ReturnRequestItem i) { return new ItemResponse(i.getId(), i.getOrderItemId(), i.getSku(), i.getQuantity(), i.getReason(), i.getResolution()); } }
    public record Response(UUID id, String returnNumber, UUID orderId, String customerId, ReturnStatus status, String comment, BigDecimal refundAmount, String refundStatus, Instant createdAt, Instant approvedAt, Instant receivedAt, Instant completedAt, List<ItemResponse> items) { static Response from(ReturnRequest r) { return new Response(r.getId(), r.getReturnNumber(), r.getOrderId(), r.getCustomerId(), r.getStatus(), r.getComment(), r.getRefundAmount(), r.getRefundStatus(), r.getCreatedAt(), r.getApprovedAt(), r.getReceivedAt(), r.getCompletedAt(), r.getItems().stream().map(ItemResponse::from).toList()); } }
}
