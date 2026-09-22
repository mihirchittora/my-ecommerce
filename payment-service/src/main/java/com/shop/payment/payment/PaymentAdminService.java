package com.shop.payment.payment;

import com.shop.payment.gateway.GatewayProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentAdminService {
    private static final List<String> SORTABLE_FIELDS = List.of(
            "createdAt", "updatedAt", "amount", "status", "provider", "refundedAmount");

    private final PaymentRepository payments;
    private final PaymentApplicationService paymentApplication;

    public PaymentAdminService(PaymentRepository payments, PaymentApplicationService paymentApplication) {
        this.payments = payments;
        this.paymentApplication = paymentApplication;
    }

    @Transactional(readOnly = true)
    public Page<PaymentDtos.AdminPaymentResponse> list(String search, PaymentStatus status,
                                                       GatewayProvider provider, String currency,
                                                       UUID orderId, UUID customerId,
                                                       Instant createdFrom, Instant createdTo,
                                                       int page, int size, String sort) {
        Specification<Payment> filter = filter(search, status, provider, currency, orderId, customerId,
                createdFrom, createdTo);
        return payments.findAll(filter, pageable(page, size, sort)).map(paymentApplication::adminResponse);
    }

    @Transactional(readOnly = true)
    public PaymentDtos.AdminPaymentResponse get(UUID paymentId) {
        return paymentApplication.adminResponse(payments.findById(paymentId)
                .orElseThrow(() -> new com.shop.payment.common.NotFoundException("Payment not found: " + paymentId)));
    }

    private Specification<Payment> filter(String search, PaymentStatus status, GatewayProvider provider,
                                          String currency, UUID orderId, UUID customerId,
                                          Instant createdFrom, Instant createdTo) {
        return (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (provider != null) predicates.add(builder.equal(root.get("provider"), provider));
            if (currency != null && !currency.isBlank()) {
                predicates.add(builder.equal(root.get("currency"), currency.trim().toUpperCase(Locale.ROOT)));
            }
            if (orderId != null) predicates.add(builder.equal(root.get("orderId"), orderId));
            if (customerId != null) predicates.add(builder.equal(root.get("customerId"), customerId));
            if (createdFrom != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            if (createdTo != null) predicates.add(builder.lessThan(root.get("createdAt"), createdTo));

            String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
            if (!normalizedSearch.isBlank()) {
                List<jakarta.persistence.criteria.Predicate> searchPredicates = new ArrayList<>();
                try {
                    UUID parsed = UUID.fromString(normalizedSearch);
                    searchPredicates.add(builder.equal(root.get("id"), parsed));
                    searchPredicates.add(builder.equal(root.get("orderId"), parsed));
                    searchPredicates.add(builder.equal(root.get("customerId"), parsed));
                } catch (IllegalArgumentException ignored) {
                    // Human-entered provider references are searched below.
                }
                String like = "%" + normalizedSearch + "%";
                searchPredicates.add(builder.like(builder.lower(root.get("providerPaymentId")), like));
                searchPredicates.add(builder.like(builder.lower(root.get("providerOrderId")), like));
                predicates.add(builder.or(searchPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Pageable pageable(int page, int size, String sort) {
        String property = "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            if (SORTABLE_FIELDS.contains(parts[0])) property = parts[0];
            if (parts.length == 2 && "asc".equalsIgnoreCase(parts[1])) direction = Sort.Direction.ASC;
        }
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, property));
    }
}
