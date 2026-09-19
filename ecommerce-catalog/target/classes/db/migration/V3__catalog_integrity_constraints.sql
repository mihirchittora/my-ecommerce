-- Keep SKU identity consistent with the application-level uppercase normalization.
CREATE UNIQUE INDEX IF NOT EXISTS uq_product_variants_sku_normalized
    ON product_variants (upper(btrim(sku)));

ALTER TABLE product_variants
    ADD CONSTRAINT ck_variants_sku_format
        CHECK (sku = upper(btrim(sku))
            AND sku ~ '^[A-Z0-9][A-Z0-9._-]{0,79}$'
            AND sku <> 'STRING') NOT VALID;

ALTER TABLE categories
    ADD CONSTRAINT ck_categories_not_own_parent
        CHECK (parent_id IS NULL OR parent_id <> id) NOT VALID;
