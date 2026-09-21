CREATE TABLE customers (
    id UUID PRIMARY KEY,
    auth_user_id UUID NOT NULL,
    first_name VARCHAR(80),
    last_name VARCHAR(80),
    email VARCHAR(320),
    phone VARCHAR(16),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_customers_auth_user_id UNIQUE (auth_user_id),
    CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED')),
    CONSTRAINT ck_customers_phone CHECK (phone IS NULL OR phone ~ '^\+[1-9][0-9]{6,14}$')
);

CREATE INDEX idx_customers_email ON customers (lower(email));
CREATE INDEX idx_customers_phone ON customers (phone);

CREATE TABLE customer_addresses (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    address_type VARCHAR(20) NOT NULL,
    recipient_name VARCHAR(120) NOT NULL,
    phone VARCHAR(16) NOT NULL,
    line1 VARCHAR(200) NOT NULL,
    line2 VARCHAR(200),
    city VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(2) NOT NULL,
    landmark VARCHAR(200),
    is_default BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT ck_customer_addresses_type CHECK (address_type IN ('SHIPPING', 'BILLING')),
    CONSTRAINT ck_customer_addresses_country CHECK (country ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_customer_addresses_phone CHECK (phone ~ '^\+[1-9][0-9]{6,14}$')
);

CREATE INDEX idx_customer_addresses_customer_id ON customer_addresses (customer_id);
CREATE INDEX idx_customer_addresses_type ON customer_addresses (address_type);
CREATE INDEX idx_customer_addresses_default ON customer_addresses (is_default);

CREATE UNIQUE INDEX uk_customer_addresses_one_default_per_type
    ON customer_addresses (customer_id, address_type)
    WHERE is_default = TRUE;
