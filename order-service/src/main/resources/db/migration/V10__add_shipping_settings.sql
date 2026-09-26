CREATE TABLE IF NOT EXISTS shipping_settings (
    id BIGINT PRIMARY KEY,
    free_shipping_threshold NUMERIC(19, 2) NOT NULL DEFAULT 500,
    standard_shipping_charge NUMERIC(19, 2) NOT NULL DEFAULT 79,
    express_shipping_charge NUMERIC(19, 2) NOT NULL DEFAULT 149,
    free_shipping_countries VARCHAR(255) NOT NULL DEFAULT 'IN',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_shipping_settings_threshold CHECK (free_shipping_threshold >= 0),
    CONSTRAINT ck_shipping_settings_standard_charge CHECK (standard_shipping_charge >= 0),
    CONSTRAINT ck_shipping_settings_express_charge CHECK (express_shipping_charge >= 0),
    CONSTRAINT ck_shipping_settings_countries CHECK (length(trim(free_shipping_countries)) > 0)
);
