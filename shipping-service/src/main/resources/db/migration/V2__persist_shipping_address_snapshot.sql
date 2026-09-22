ALTER TABLE fulfillments
    ADD COLUMN shipping_recipient_name VARCHAR(120),
    ADD COLUMN shipping_phone VARCHAR(30),
    ADD COLUMN shipping_line1 VARCHAR(200),
    ADD COLUMN shipping_line2 VARCHAR(200),
    ADD COLUMN shipping_city VARCHAR(120),
    ADD COLUMN shipping_state VARCHAR(120),
    ADD COLUMN shipping_postal_code VARCHAR(20),
    ADD COLUMN shipping_country VARCHAR(2),
    ADD COLUMN shipping_landmark VARCHAR(200);

ALTER TABLE fulfillments
    ADD CONSTRAINT ck_fulfillments_shipping_country
    CHECK (shipping_country IS NULL OR shipping_country ~ '^[A-Z]{2}$');
