CREATE TABLE site_settings (
    id UUID PRIMARY KEY,
    site_title VARCHAR(160) NOT NULL,
    logo_url VARCHAR(1000),
    logo_storage_key VARCHAR(1000),
    logo_original_filename VARCHAR(255),
    logo_content_type VARCHAR(100),
    logo_size_bytes BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE carousel_slides (
    id UUID PRIMARY KEY,
    site_settings_id UUID NOT NULL,
    sort_order INTEGER NOT NULL,
    eyebrow VARCHAR(160),
    headline VARCHAR(300) NOT NULL,
    description VARCHAR(1000),
    primary_cta_label VARCHAR(80),
    primary_cta_url VARCHAR(500),
    secondary_cta_label VARCHAR(80),
    secondary_cta_url VARCHAR(500),
    image_url VARCHAR(1000),
    image_storage_key VARCHAR(1000),
    image_original_filename VARCHAR(255),
    image_content_type VARCHAR(100),
    image_size_bytes BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_carousel_slides_settings
        FOREIGN KEY (site_settings_id) REFERENCES site_settings(id) ON DELETE CASCADE,
    CONSTRAINT ck_carousel_slides_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX idx_carousel_slides_settings_order
    ON carousel_slides(site_settings_id, sort_order);

INSERT INTO site_settings (id, site_title, updated_at)
VALUES ('6d3c5c7c-13ad-4bca-9f36-6e1c7a700001', 'Morrow', CURRENT_TIMESTAMP);

INSERT INTO carousel_slides (
    id, site_settings_id, sort_order, eyebrow, headline, description,
    primary_cta_label, primary_cta_url, secondary_cta_label, secondary_cta_url,
    active, created_at, updated_at
)
VALUES
    (
        '6d3c5c7c-13ad-4bca-9f36-6e1c7a700101',
        '6d3c5c7c-13ad-4bca-9f36-6e1c7a700001',
        0,
        'Thoughtful shopping, made simple',
        'Everyday goods, at prices that feel good.',
        'Browse useful, beautiful pieces across the categories you reach for most.',
        'Shop all products', '/products', 'Explore categories', '/categories',
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        '6d3c5c7c-13ad-4bca-9f36-6e1c7a700102',
        '6d3c5c7c-13ad-4bca-9f36-6e1c7a700001',
        1,
        'Make room for good things',
        'Small upgrades for everyday rituals.',
        'Find considered pieces that make home feel a little more like yours.',
        'Shop home', '/categories', 'See all products', '/products',
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        '6d3c5c7c-13ad-4bca-9f36-6e1c7a700103',
        '6d3c5c7c-13ad-4bca-9f36-6e1c7a700001',
        2,
        'Better finds, less noise',
        'The useful things worth bringing home.',
        'A calmer catalogue of practical favourites, ready when you are.',
        'Browse the collection', '/products', 'Shop by category', '/categories',
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    );
