ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(7, 4) NOT NULL DEFAULT 18;

ALTER TABLE product_variants
    ADD CONSTRAINT ck_variants_tax_rate
        CHECK (tax_rate >= 0 AND tax_rate <= 100) NOT VALID;
