CREATE TABLE order_shipping_addresses (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    source_address_id UUID,
    recipient_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    line1 VARCHAR(200) NOT NULL,
    line2 VARCHAR(200),
    city VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(2) NOT NULL,
    landmark VARCHAR(200),
    snapshotted_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_order_shipping_addresses_country CHECK (country ~ '^[A-Z]{2}$')
);

CREATE INDEX idx_order_shipping_addresses_source ON order_shipping_addresses(source_address_id);
