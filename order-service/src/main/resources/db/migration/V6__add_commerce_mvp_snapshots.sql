alter table orders add column taxable_amount numeric(19,2) not null default 0;
alter table orders add column tax_rate numeric(7,4) not null default 0;
alter table orders add column coupon_code varchar(40);
alter table order_items add column discount_amount numeric(19,2) not null default 0;
alter table order_items add column taxable_amount numeric(19,2) not null default 0;
alter table order_items add column tax_amount numeric(19,2) not null default 0;

create table coupons (
    id uuid primary key,
    code varchar(40) not null unique,
    type varchar(20) not null,
    value numeric(19,2) not null,
    minimum_order_amount numeric(19,2) not null default 0,
    maximum_discount numeric(19,2),
    starts_at timestamptz,
    expires_at timestamptz,
    usage_limit bigint,
    per_customer_limit bigint,
    usage_count bigint not null default 0,
    active boolean not null default true,
    version bigint not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint ck_coupons_type check (type in ('PERCENTAGE', 'FIXED')),
    constraint ck_coupons_value check (value >= 0),
    constraint ck_coupons_minimum check (minimum_order_amount >= 0),
    constraint ck_coupons_usage check (usage_count >= 0 and (usage_limit is null or usage_limit >= 0))
);

create table coupon_redemptions (
    id uuid primary key,
    coupon_id uuid not null references coupons(id),
    customer_id varchar(200) not null,
    -- Order Service writes the redemption in the coupon transaction before the
    -- surrounding checkout transaction persists the order. Keep the business
    -- identifier here without a database FK so coupon usage cannot deadlock
    -- checkout ordering; the application still validates the order ownership.
    order_id uuid not null unique,
    created_at timestamptz not null
);
create index ix_coupon_redemptions_customer on coupon_redemptions(coupon_id, customer_id);
