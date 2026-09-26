package com.shop.order.returns;

import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderItem;
import com.shop.order.domain.OrderRepository;
import com.shop.order.domain.OrderStatus;
import com.shop.order.exception.ConflictException;
import com.shop.order.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReturnService {
    private final ReturnRequestRepository returns; private final OrderRepository orders; private final ReturnNumberGenerator numbers; private final PaymentRefundClient refunds; private final int returnWindowDays;
    public ReturnService(ReturnRequestRepository returns, OrderRepository orders, ReturnNumberGenerator numbers, PaymentRefundClient refunds, @Value("${app.returns.window-days:30}") int returnWindowDays) { this.returns = returns; this.orders = orders; this.numbers = numbers; this.refunds = refunds; this.returnWindowDays = returnWindowDays; }

    @Transactional
    public ReturnDtos.Response create(ReturnDtos.CreateRequest request, Authentication authentication) {
        CustomerOrder order = orders.findDetailedById(request.orderId()).orElseThrow(() -> new NotFoundException("Order not found"));
        if (!order.getCustomerId().equals(authentication.getName())) throw new NotFoundException("Order not found");
        if (!(order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED) || order.getUpdatedAt().isBefore(Instant.now().minus(returnWindowDays, ChronoUnit.DAYS))) throw new ConflictException("This order is not eligible for return");
        Map<UUID, OrderItem> byId = new HashMap<>(); order.getItems().forEach(item -> byId.put(item.getId(), item));
        Map<UUID, Long> alreadyReturned = alreadyReturned(request.orderId());
        Map<UUID, Long> requestedNow = new HashMap<>();
        ReturnRequest result = new ReturnRequest(); result.setId(UUID.randomUUID()); result.setReturnNumber(numbers.next()); result.setOrderId(order.getId()); result.setCustomerId(order.getCustomerId()); result.setStatus(ReturnStatus.REQUESTED); result.setComment(request.comment() == null ? "" : request.comment().trim());
        for (ReturnDtos.ItemRequest itemRequest : request.items()) {
            OrderItem item = byId.get(itemRequest.orderItemId());
            long requestedQuantity = requestedNow.merge(itemRequest.orderItemId(), itemRequest.quantity(), Long::sum);
            if (item == null || requestedQuantity > item.getQuantity() - alreadyReturned.getOrDefault(item.getId(), 0L)) throw new ConflictException("Return quantity is not eligible for item");
            ReturnRequestItem itemResult = new ReturnRequestItem(); itemResult.setOrderItemId(item.getId()); itemResult.setSku(item.getSku()); itemResult.setQuantity(itemRequest.quantity()); itemResult.setReason(itemRequest.reason()); itemResult.setResolution(itemRequest.resolution()); result.addItem(itemResult);
        }
        return ReturnDtos.Response.from(returns.save(result));
    }
    @Transactional(readOnly = true) public Page<ReturnDtos.Response> mine(Authentication authentication, Pageable pageable) { return returns.findByCustomerId(authentication.getName(), pageable).map(ReturnDtos.Response::from); }
    @Transactional(readOnly = true) public ReturnDtos.Response get(UUID id, Authentication authentication, boolean admin) { ReturnRequest result = returns.findById(id).orElseThrow(() -> new NotFoundException("Return request not found")); if (!admin && !result.getCustomerId().equals(authentication.getName())) throw new NotFoundException("Return request not found"); return ReturnDtos.Response.from(result); }
    @Transactional(readOnly = true) public Page<ReturnDtos.Response> adminList(ReturnStatus status, Pageable pageable) { return status == null ? returns.findAll(pageable).map(ReturnDtos.Response::from) : returns.findAllByStatus(status, pageable).map(ReturnDtos.Response::from); }
    @Transactional public ReturnDtos.Response transition(UUID id, ReturnStatus target) { ReturnRequest result = returns.findById(id).orElseThrow(() -> new NotFoundException("Return request not found")); if (!allowed(result.getStatus(), target)) throw new ConflictException("Return cannot transition from " + result.getStatus() + " to " + target); result.setStatus(target); Instant now = Instant.now(); if (target == ReturnStatus.APPROVED) result.setApprovedAt(now); if (target == ReturnStatus.RECEIVED) result.setReceivedAt(now); if (target == ReturnStatus.COMPLETED) { result.setCompletedAt(now); result.setRefundAmount(refundAmount(result)); result.setRefundStatus(refunds.request(result.getOrderId(), result.getRefundAmount(), result.getReturnNumber(), "Return " + result.getReturnNumber()) ? "REQUESTED" : "REFUND_PENDING"); } return ReturnDtos.Response.from(returns.save(result)); }
    private boolean allowed(ReturnStatus from, ReturnStatus to) { return switch (from) { case REQUESTED -> to == ReturnStatus.APPROVED || to == ReturnStatus.REJECTED || to == ReturnStatus.CANCELLED; case APPROVED -> to == ReturnStatus.IN_TRANSIT || to == ReturnStatus.RECEIVED || to == ReturnStatus.CANCELLED; case IN_TRANSIT -> to == ReturnStatus.RECEIVED; case RECEIVED -> to == ReturnStatus.COMPLETED; default -> false; }; }
    private Map<UUID, Long> alreadyReturned(UUID orderId) { Map<UUID, Long> values = new HashMap<>(); returns.findByOrderId(orderId).stream().filter(r -> r.getStatus() != ReturnStatus.REJECTED && r.getStatus() != ReturnStatus.CANCELLED).flatMap(r -> r.getItems().stream()).forEach(item -> values.merge(item.getOrderItemId(), item.getQuantity(), Long::sum)); return values; }
    private BigDecimal refundAmount(ReturnRequest request) { CustomerOrder order = orders.findDetailedById(request.getOrderId()).orElseThrow(() -> new NotFoundException("Order not found")); BigDecimal total = BigDecimal.ZERO; for (ReturnRequestItem returned : request.getItems()) { OrderItem item = order.getItems().stream().filter(candidate -> candidate.getId().equals(returned.getOrderItemId())).findFirst().orElseThrow(); total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(returned.getQuantity())).subtract(item.getDiscountAmount().multiply(BigDecimal.valueOf(returned.getQuantity())).divide(BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP)).add(item.getTaxAmount().multiply(BigDecimal.valueOf(returned.getQuantity())).divide(BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP))); } return total.setScale(2, RoundingMode.HALF_UP).min(order.getTotalAmount()); }
}
