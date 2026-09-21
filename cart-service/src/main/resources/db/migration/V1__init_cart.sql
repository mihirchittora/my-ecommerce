CREATE TABLE carts (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    checkout_idempotency_key VARCHAR(200),
    converted_order_id UUID,
    converted_order_number VARCHAR(80),
    CONSTRAINT ck_carts_status CHECK (status IN ('ACTIVE', 'CHECKOUT_IN_PROGRESS', 'CONVERTED', 'ABANDONED', 'EXPIRED')),
    CONSTRAINT ck_carts_currency CHECK (currency = upper(currency) AND length(currency) = 3)
);

CREATE UNIQUE INDEX uq_carts_one_active_per_customer
    ON carts(customer_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_carts_customer_id ON carts(customer_id);
CREATE INDEX idx_carts_status ON carts(status);
CREATE INDEX idx_carts_expires_at ON carts(expires_at);

CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    sku VARCHAR(80) NOT NULL,
    quantity BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_cart_items_sku CHECK (length(trim(sku)) > 0),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0),
    CONSTRAINT uq_cart_items_cart_sku UNIQUE (cart_id, sku)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_sku ON cart_items(sku);
