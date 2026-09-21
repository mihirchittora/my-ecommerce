create sequence order_number_sequence start with 1 increment by 1;

create table orders (
    id uuid primary key,
    version bigint not null default 0,
    order_number varchar(40) not null unique,
    customer_id varchar(200) not null,
    idempotency_key varchar(200),
    idempotency_payload_hash varchar(64),
    status varchar(30) not null,
    currency varchar(3) not null,
    subtotal numeric(19,2) not null,
    discount_amount numeric(19,2) not null,
    shipping_amount numeric(19,2) not null,
    tax_amount numeric(19,2) not null,
    total_amount numeric(19,2) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    cancelled_at timestamptz,
    completed_at timestamptz,
    constraint ck_orders_amounts_nonnegative check (
        subtotal >= 0 and discount_amount >= 0 and shipping_amount >= 0
        and tax_amount >= 0 and total_amount >= 0
    )
);

create table order_items (
    id uuid primary key,
    order_id uuid not null references orders(id),
    sku varchar(80) not null,
    product_name_snapshot varchar(300) not null,
    variant_snapshot jsonb not null default '{}'::jsonb,
    unit_price numeric(19,2) not null,
    currency varchar(3) not null,
    quantity bigint not null,
    subtotal numeric(19,2) not null,
    reservation_id uuid,
    reservation_reference varchar(220),
    created_at timestamptz not null,
    constraint ck_order_items_quantity_positive check (quantity > 0),
    constraint ck_order_items_amounts_nonnegative check (unit_price >= 0 and subtotal >= 0)
);

create table order_history (
    id uuid primary key,
    order_id uuid not null references orders(id) on delete cascade,
    from_status varchar(30),
    to_status varchar(30) not null,
    event_type varchar(50) not null,
    reference_id varchar(220),
    notes varchar(1000),
    actor_user_id varchar(200),
    created_at timestamptz not null
);

create table order_item_inventory_units (
    id uuid primary key,
    order_item_id uuid not null references order_items(id) on delete cascade,
    reservation_id uuid not null,
    inventory_unit_id uuid not null,
    unique (order_item_id, reservation_id, inventory_unit_id)
);

create table order_idempotency (
    id uuid primary key,
    customer_id varchar(200) not null,
    idempotency_key varchar(200) not null,
    request_hash varchar(64) not null,
    order_id uuid unique references orders(id),
    created_at timestamptz not null,
    constraint uq_order_idempotency_customer_key unique (customer_id, idempotency_key)
);

create index ix_orders_customer_id on orders(customer_id);
create index ix_orders_status on orders(status);
create index ix_orders_created_at on orders(created_at);
create index ix_orders_status_created_at on orders(status, created_at);
create index ix_order_items_order_id on order_items(order_id);
create index ix_order_items_sku on order_items(sku);
create index ix_order_history_order_id on order_history(order_id);
create index ix_order_history_created_at on order_history(created_at);
create index ix_order_idempotency_lookup on order_idempotency(customer_id, idempotency_key);
