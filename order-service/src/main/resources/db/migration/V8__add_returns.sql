create sequence return_number_sequence start with 1001 increment by 1;
create table return_requests (
    id uuid primary key,
    return_number varchar(50) not null unique,
    order_id uuid not null references orders(id),
    customer_id varchar(200) not null,
    status varchar(20) not null,
    comment varchar(1000) not null,
    refund_amount numeric(19,2) not null default 0,
    refund_status varchar(30) not null default 'NOT_REQUESTED',
    created_at timestamptz not null,
    approved_at timestamptz,
    received_at timestamptz,
    completed_at timestamptz,
    constraint ck_return_status check(status in ('REQUESTED','APPROVED','REJECTED','IN_TRANSIT','RECEIVED','COMPLETED','CANCELLED'))
);
create table return_request_items (
    id uuid primary key,
    return_request_id uuid not null references return_requests(id) on delete cascade,
    order_item_id uuid not null references order_items(id),
    sku varchar(80) not null,
    quantity bigint not null,
    reason varchar(30) not null,
    resolution varchar(30),
    constraint ck_return_item_quantity check(quantity > 0)
);
create index ix_returns_customer_created on return_requests(customer_id, created_at desc);
create index ix_returns_order on return_requests(order_id);
