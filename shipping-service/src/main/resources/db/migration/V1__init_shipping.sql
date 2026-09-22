CREATE SEQUENCE shipment_number_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE fulfillments (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    order_id UUID NOT NULL UNIQUE,
    order_number VARCHAR(40) NOT NULL,
    customer_id VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    shipping_address_reference VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT ck_fulfillments_status CHECK (status IN ('PENDING','READY','ALLOCATING','PACKED','PARTIALLY_SHIPPED','SHIPPED','DELIVERED','COMPLETED','CANCELLED','FAILED'))
);

CREATE TABLE fulfillment_items (
    id UUID PRIMARY KEY,
    fulfillment_id UUID NOT NULL REFERENCES fulfillments(id) ON DELETE CASCADE,
    order_item_id UUID NOT NULL,
    sku VARCHAR(80) NOT NULL,
    product_name_snapshot VARCHAR(300) NOT NULL,
    quantity BIGINT NOT NULL,
    reservation_id UUID,
    inventory_unit_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    inventory_unit_codes JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_fulfillment_items_order_item UNIQUE (fulfillment_id, order_item_id),
    CONSTRAINT ck_fulfillment_items_quantity CHECK (quantity > 0)
);

CREATE TABLE shipments (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    shipment_number VARCHAR(40) NOT NULL UNIQUE,
    fulfillment_id UUID NOT NULL REFERENCES fulfillments(id),
    order_id UUID NOT NULL,
    order_number VARCHAR(40) NOT NULL,
    customer_id VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    carrier VARCHAR(50) NOT NULL,
    service_level VARCHAR(50) NOT NULL,
    tracking_number VARCHAR(120),
    provider_shipment_id VARCHAR(200),
    label_reference VARCHAR(500),
    shipping_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    package_count INTEGER NOT NULL DEFAULT 1,
    estimated_delivery_at TIMESTAMPTZ,
    order_notification_pending BOOLEAN NOT NULL DEFAULT FALSE,
    order_notification_last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    shipped_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT ck_shipments_cost CHECK (shipping_cost >= 0),
    CONSTRAINT ck_shipments_package_count CHECK (package_count > 0),
    CONSTRAINT ck_shipments_status CHECK (status IN ('CREATED','READY','PACKED','SHIPPED','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','DELIVERY_FAILED','RETURNED','CANCELLED','FAILED'))
);

CREATE TABLE shipment_items (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    order_item_id UUID NOT NULL,
    sku VARCHAR(80) NOT NULL,
    product_name_snapshot VARCHAR(300) NOT NULL,
    quantity BIGINT NOT NULL,
    inventory_unit_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_shipment_items_quantity CHECK (quantity > 0)
);

CREATE TABLE shipment_tracking_events (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    tracking_number VARCHAR(120),
    carrier VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_status VARCHAR(50) NOT NULL,
    event_location VARCHAR(200),
    description VARCHAR(1000),
    provider_event_id VARCHAR(200),
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_tracking_provider_event UNIQUE (carrier, provider_event_id)
);

CREATE TABLE shipment_history (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    carrier_event_id VARCHAR(200),
    reference_id VARCHAR(220),
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE fulfillment_history (
    id UUID PRIMARY KEY,
    fulfillment_id UUID NOT NULL REFERENCES fulfillments(id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(220),
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE shipment_idempotency (
    id UUID PRIMARY KEY,
    fulfillment_id UUID NOT NULL REFERENCES fulfillments(id),
    idempotency_key VARCHAR(200) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    shipment_id UUID REFERENCES shipments(id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_shipment_idempotency UNIQUE (fulfillment_id, idempotency_key)
);

CREATE TABLE carrier_webhook_events (
    id UUID PRIMARY KEY,
    carrier VARCHAR(50) NOT NULL,
    provider_event_id VARCHAR(200) NOT NULL,
    provider_shipment_id VARCHAR(200),
    event_type VARCHAR(50) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    ignored_reason VARCHAR(500),
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_carrier_webhook_event UNIQUE (carrier, provider_event_id)
);

CREATE INDEX ix_fulfillments_customer_created ON fulfillments(customer_id, created_at);
CREATE INDEX ix_fulfillments_status ON fulfillments(status);
CREATE INDEX ix_fulfillment_items_fulfillment ON fulfillment_items(fulfillment_id);
CREATE INDEX ix_shipments_fulfillment ON shipments(fulfillment_id);
CREATE INDEX ix_shipments_customer_created ON shipments(customer_id, created_at);
CREATE INDEX ix_shipments_status ON shipments(status);
CREATE INDEX ix_shipments_tracking ON shipments(tracking_number);
CREATE INDEX ix_shipment_items_shipment ON shipment_items(shipment_id);
CREATE INDEX ix_shipment_items_inventory_unit ON shipment_items(inventory_unit_id);
CREATE INDEX ix_tracking_events_shipment_occurred ON shipment_tracking_events(shipment_id, occurred_at);
CREATE INDEX ix_shipment_history_shipment_created ON shipment_history(shipment_id, created_at);
CREATE INDEX ix_fulfillment_history_fulfillment_created ON fulfillment_history(fulfillment_id, created_at);
