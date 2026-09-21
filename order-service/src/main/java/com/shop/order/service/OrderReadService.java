package com.shop.order.service;

import com.shop.order.api.OrderDtos;
import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderRepository;
import com.shop.order.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrderReadService {
    private final OrderRepository orders;

    public OrderReadService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse response(UUID orderId, boolean operationalReferences) {
        CustomerOrder order = detailed(orderId);
        return OrderDtos.response(order, operationalReferences);
    }

    @Transactional(readOnly = true)
    public CustomerOrder detailed(UUID orderId) {
        CustomerOrder order = orders.findDetailedById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        order.getItems().forEach(item -> item.getInventoryUnitReferences().size());
        order.getHistory().size();
        return order;
    }

    @Transactional(readOnly = true)
    public Page<OrderDtos.OrderSummaryResponse> myOrders(String customerId, Pageable pageable) {
        return orders.findByCustomerId(customerId, pageable).map(OrderDtos.OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderDtos.OrderSummaryResponse> search(com.shop.order.domain.OrderStatus status,
                                                       String orderNumber, String customerId, String sku,
                                                       java.time.Instant createdFrom, java.time.Instant createdTo,
                                                       Pageable pageable) {
        Specification<CustomerOrder> specification = (root, query, criteriaBuilder) -> {
            query.distinct(true);
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(criteriaBuilder.equal(root.get("status"), status));
            if (orderNumber != null) {
                String pattern = "%" + orderNumber.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("orderNumber")), pattern));
            }
            if (customerId != null) predicates.add(criteriaBuilder.equal(root.get("customerId"), customerId));
            if (sku != null) {
                var item = root.join("items", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(item.get("sku"), sku));
            }
            if (createdFrom != null) predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            if (createdTo != null) predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), createdTo));
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return orders.findAll(specification, pageable)
                .map(OrderDtos.OrderSummaryResponse::from);
    }
}
