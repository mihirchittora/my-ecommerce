package com.shop.cart.repository;

import com.shop.cart.domain.Cart;
import com.shop.cart.domain.CartStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByCustomerIdAndStatus(String customerId, CartStatus status);

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByIdAndCustomerId(UUID id, String customerId);

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByCustomerIdAndCheckoutIdempotencyKey(String customerId, String checkoutIdempotencyKey);

    @EntityGraph(attributePaths = "items")
    @Query("select distinct c from Cart c left join c.items item "
            + "where (:cartId is null or c.id = :cartId) "
            + "and (coalesce(:search, '') = '' or lower(c.customerId) like lower(concat('%', coalesce(:search, ''), '%'))) "
            + "and (:status is null or c.status = :status) "
            + "and (:sku is null or item.sku = :sku)")
    Page<Cart> searchForStaff(@Param("cartId") UUID cartId,
                              @Param("search") String search,
                              @Param("status") CartStatus status,
                              @Param("sku") String sku,
                              Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<Cart> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct c from Cart c left join fetch c.items where c.id = :id and c.customerId = :customerId")
    Optional<Cart> findByIdAndCustomerIdForUpdate(@Param("id") UUID id, @Param("customerId") String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct c from Cart c left join fetch c.items where c.customerId = :customerId and c.status = :status")
    Optional<Cart> findByCustomerIdAndStatusForUpdate(@Param("customerId") String customerId,
                                                       @Param("status") CartStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.status = :status and c.expiresAt is not null and c.expiresAt <= :now")
    List<Cart> findExpiredForUpdate(@Param("status") CartStatus status, @Param("now") Instant now);
}
