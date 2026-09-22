create table payments (
    id uuid primary key,
    version bigint not null default 0,
    order_id uuid not null,
    customer_id uuid not null,
    amount numeric(19,2) not null,
    currency varchar(3) not null,
    status varchar(30) not null,
    provider varchar(40) not null,
    provider_payment_id varchar(200),
    provider_order_id varchar(200),
    checkout_url varchar(1000),
    checkout_token varchar(500),
    idempotency_key varchar(200) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    authorized_at timestamptz,
    captured_at timestamptz,
    failed_at timestamptz,
    cancelled_at timestamptz,
    refunded_amount numeric(19,2) not null default 0,
    constraint ck_payments_amount_positive check (amount > 0),
    constraint ck_payments_refunded_nonnegative check (refunded_amount >= 0 and refunded_amount <= amount),
    constraint ck_payments_currency_iso check (currency ~ '^[A-Z]{3}$')
);

create unique index uq_payments_customer_idempotency on payments(customer_id, idempotency_key);
create index ix_payments_order_id on payments(order_id);
create index ix_payments_provider_payment_id on payments(provider, provider_payment_id);
create index ix_payments_provider_order_id on payments(provider, provider_order_id);
create index ix_payments_status on payments(status);

create table payment_attempts (
    id uuid primary key,
    payment_id uuid not null references payments(id) on delete cascade,
    attempt_number integer not null,
    idempotency_key varchar(200) not null,
    provider varchar(40) not null,
    status varchar(30) not null,
    provider_payment_id varchar(200),
    provider_order_id varchar(200),
    failure_code varchar(100),
    failure_message varchar(1000),
    amount numeric(19,2) not null,
    currency varchar(3) not null,
    started_at timestamptz not null,
    completed_at timestamptz,
    created_at timestamptz not null,
    constraint uq_payment_attempt_number unique(payment_id, attempt_number),
    constraint uq_payment_attempt_idempotency unique(payment_id, idempotency_key),
    constraint ck_payment_attempt_amount_positive check (amount > 0)
);

create index ix_payment_attempts_payment_id on payment_attempts(payment_id);

create table refunds (
    id uuid primary key,
    payment_id uuid not null references payments(id) on delete cascade,
    amount numeric(19,2) not null,
    currency varchar(3) not null,
    status varchar(30) not null,
    provider_refund_id varchar(200),
    idempotency_key varchar(200) not null,
    reason varchar(500),
    failure_code varchar(100),
    failure_message varchar(1000),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    completed_at timestamptz,
    constraint uq_refund_payment_idempotency unique(payment_id, idempotency_key),
    constraint ck_refund_amount_positive check (amount > 0)
);

create index ix_refunds_payment_id on refunds(payment_id);

create table webhook_events (
    id uuid primary key,
    provider varchar(40) not null,
    provider_event_id varchar(240) not null,
    event_type varchar(100) not null,
    provider_payment_id varchar(200),
    provider_order_id varchar(200),
    payload_hash varchar(64) not null,
    status varchar(30) not null,
    received_at timestamptz not null,
    processed_at timestamptz,
    failure_message varchar(1000),
    constraint uq_webhook_provider_event unique(provider, provider_event_id)
);

create index ix_webhook_events_payment on webhook_events(provider, provider_payment_id);
