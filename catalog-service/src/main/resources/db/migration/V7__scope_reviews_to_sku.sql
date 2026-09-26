alter table product_reviews add column if not exists sku varchar(80);
alter table product_reviews drop constraint if exists uk_review_customer_product;
alter table product_reviews add constraint uk_review_customer_product_sku unique(customer_id, product_id, sku);
create index ix_product_reviews_public_sku on product_reviews(product_id, sku, status, created_at desc);
