CREATE TABLE fulfillment_outbox (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_fulfillment_outbox_status CHECK (status IN ('PENDING','COMPLETED'))
);

CREATE INDEX ix_fulfillment_outbox_status_created ON fulfillment_outbox(status, created_at);
