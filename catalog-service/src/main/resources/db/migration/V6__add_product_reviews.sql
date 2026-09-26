create table product_reviews (
    id uuid primary key,
    product_id uuid not null references products(id) on delete cascade,
    customer_id varchar(200) not null,
    order_id uuid not null,
    order_item_id uuid not null,
    rating integer not null,
    title varchar(160),
    comment varchar(4000) not null,
    status varchar(20) not null,
    verified_purchase boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_review_customer_product unique(customer_id, product_id),
    constraint ck_review_rating check(rating between 1 and 5),
    constraint ck_review_status check(status in ('PENDING','APPROVED','REJECTED'))
);
create index ix_product_reviews_public on product_reviews(product_id, status, created_at desc);
