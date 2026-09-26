create table wishlist_items (
    id uuid primary key,
    customer_id uuid not null references customers(id) on delete cascade,
    product_id uuid not null,
    sku varchar(80),
    created_at timestamptz not null,
    constraint uk_wishlist_customer_product_sku unique(customer_id, product_id, sku)
);
create unique index uk_wishlist_customer_product_without_sku
    on wishlist_items(customer_id, product_id) where sku is null;
create index ix_wishlist_customer on wishlist_items(customer_id, created_at desc);
