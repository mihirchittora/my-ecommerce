-- Database-level protection. NOT VALID keeps existing local test rows intact while
-- enforcing the rule for all new or updated rows.
ALTER TABLE product_variants
    ADD CONSTRAINT ck_variants_price_positive CHECK (price > 0) NOT VALID;

ALTER TABLE product_variants
    ADD CONSTRAINT ck_variants_currency_supported
        CHECK (currency IN ('INR', 'USD', 'EUR', 'GBP', 'JPY', 'AUD', 'CAD', 'SGD')) NOT VALID;

ALTER TABLE product_variants
    ADD CONSTRAINT ck_variants_sku_not_blank CHECK (btrim(sku) <> '') NOT VALID;

CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_root_name_lower
    ON categories (lower(name)) WHERE parent_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_parent_name_lower
    ON categories (parent_id, lower(name)) WHERE parent_id IS NOT NULL;

ALTER TABLE product_images
    ADD COLUMN IF NOT EXISTS variant_id UUID NULL,
    ADD COLUMN IF NOT EXISTS storage_key VARCHAR(1000) NULL,
    ADD COLUMN IF NOT EXISTS original_filename VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS size_bytes BIGINT NULL;

ALTER TABLE product_images
    ADD CONSTRAINT fk_images_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_images_variant_id ON product_images(variant_id);
CREATE INDEX IF NOT EXISTS idx_products_name_lower ON products(lower(name));
