ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS description VARCHAR(500);

CREATE TABLE category_images (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL UNIQUE,
    url VARCHAR(1000) NOT NULL,
    storage_key VARCHAR(1000) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    alt_text VARCHAR(255) NOT NULL,
    CONSTRAINT fk_category_images_category
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    CONSTRAINT ck_category_images_size CHECK (size_bytes > 0)
);
