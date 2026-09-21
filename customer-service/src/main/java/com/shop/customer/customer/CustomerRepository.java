package com.shop.customer.customer;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByAuthUserId(UUID authUserId);

    @Query("""
            select c from Customer c
            where (:status is null or c.status = :status)
              and (:search = '' or
                   lower(coalesce(c.firstName, '')) like lower(concat('%', :search, '%')) or
                   lower(coalesce(c.lastName, '')) like lower(concat('%', :search, '%')) or
                   lower(coalesce(c.email, '')) like lower(concat('%', :search, '%')) or
                   cast(c.authUserId as string) like concat('%', :search, '%'))
            """)
    Page<Customer> search(@Param("search") String search, @Param("status") CustomerStatus status, Pageable pageable);

    @Modifying
    @Query(value = """
            INSERT INTO customers (id, auth_user_id, status, created_at, updated_at)
            VALUES (:id, :authUserId, 'ACTIVE', :createdAt, :updatedAt)
            ON CONFLICT (auth_user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("authUserId") UUID authUserId,
                       @Param("createdAt") java.time.Instant createdAt,
                       @Param("updatedAt") java.time.Instant updatedAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.authUserId = :authUserId")
    Optional<Customer> findByAuthUserIdForUpdate(@Param("authUserId") UUID authUserId);
}
