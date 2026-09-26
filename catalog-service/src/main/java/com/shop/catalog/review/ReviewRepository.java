package com.shop.catalog.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Review> {
    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(UUID productId, ReviewStatus status, Pageable pageable);
    Page<Review> findByProductIdAndSkuAndStatusOrderByCreatedAtDesc(UUID productId, String sku, ReviewStatus status, Pageable pageable);
    Optional<Review> findByIdAndCustomerId(UUID id, String customerId);
    Optional<Review> findByCustomerIdAndProductIdAndSku(String customerId, UUID productId, String sku);
    Optional<Review> findByCustomerIdAndProductIdAndOrderItemId(String customerId, UUID productId, UUID orderItemId);
    boolean existsByCustomerIdAndProductId(String customerId, UUID productId);
    boolean existsByCustomerIdAndProductIdAndSku(String customerId, UUID productId, String sku);
    @Query("select avg(r.rating) from Review r where r.productId = :productId and r.status = 'APPROVED'")
    Double averageForProduct(@Param("productId") UUID productId);
    @Query("select avg(r.rating) from Review r where r.productId = :productId and r.sku = :sku and r.status = 'APPROVED'")
    Double averageForProductAndSku(@Param("productId") UUID productId, @Param("sku") String sku);
    long countByProductIdAndStatus(UUID productId, ReviewStatus status);
    long countByProductIdAndSkuAndStatus(UUID productId, String sku, ReviewStatus status);
    @Query("select r.rating, count(r) from Review r where r.productId = :productId and r.status = 'APPROVED' group by r.rating order by r.rating")
    java.util.List<Object[]> distributionForProduct(@Param("productId") UUID productId);
    @Query("select r.rating, count(r) from Review r where r.productId = :productId and r.sku = :sku and r.status = 'APPROVED' group by r.rating order by r.rating")
    java.util.List<Object[]> distributionForProductAndSku(@Param("productId") UUID productId, @Param("sku") String sku);
}
