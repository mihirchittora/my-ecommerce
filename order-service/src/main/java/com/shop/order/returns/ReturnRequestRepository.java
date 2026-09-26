package com.shop.order.returns;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {
    Page<ReturnRequest> findByCustomerId(String customerId, Pageable pageable);
    Page<ReturnRequest> findAllByStatus(ReturnStatus status, Pageable pageable);
    List<ReturnRequest> findByOrderId(UUID orderId);
}
