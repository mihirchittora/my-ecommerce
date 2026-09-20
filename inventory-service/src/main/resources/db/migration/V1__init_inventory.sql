CREATE TABLE inventory_locations (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_inventory_locations_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    sku VARCHAR(80) NOT NULL,
    location_id UUID NOT NULL,
    quantity BIGINT NOT NULL,
    reserved_quantity BIGINT NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_inventory_items_sku_location UNIQUE (sku, location_id),
    CONSTRAINT fk_inventory_items_location FOREIGN KEY (location_id) REFERENCES inventory_locations(id),
    CONSTRAINT ck_inventory_items_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_inventory_items_reserved CHECK (reserved_quantity >= 0 AND reserved_quantity <= quantity)
);

CREATE INDEX idx_inventory_items_sku ON inventory_items(sku);
CREATE INDEX idx_inventory_items_location ON inventory_items(location_id);

CREATE TABLE inventory_units (
    id UUID PRIMARY KEY,
    inventory_item_id UUID NOT NULL,
    unit_code VARCHAR(60) NOT NULL UNIQUE,
    sku VARCHAR(80) NOT NULL,
    receipt_reference_id VARCHAR(200),
    serial_number VARCHAR(150),
    imei VARCHAR(30),
    barcode VARCHAR(150),
    status VARCHAR(20) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sold_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_inventory_units_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT ck_inventory_units_status CHECK (status IN ('AVAILABLE', 'RESERVED', 'ALLOCATED', 'IN_TRANSIT', 'SOLD', 'RETURNED', 'DAMAGED', 'LOST'))
);

CREATE INDEX idx_inventory_units_sku_location_status ON inventory_units(sku, inventory_item_id, status);
CREATE INDEX idx_inventory_units_receipt_reference ON inventory_units(sku, receipt_reference_id);
CREATE INDEX idx_inventory_units_serial ON inventory_units(serial_number);
CREATE INDEX idx_inventory_units_imei ON inventory_units(imei);
CREATE INDEX idx_inventory_units_barcode ON inventory_units(barcode);

CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY,
    sku VARCHAR(80) NOT NULL,
    location_id UUID NOT NULL,
    quantity BIGINT NOT NULL,
    reference_id VARCHAR(200) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_inventory_reservations_location FOREIGN KEY (location_id) REFERENCES inventory_locations(id),
    CONSTRAINT ck_inventory_reservations_quantity CHECK (quantity > 0),
    CONSTRAINT ck_inventory_reservations_status CHECK (status IN ('ACTIVE', 'CONFIRMED', 'RELEASED', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX idx_inventory_reservations_expiry ON inventory_reservations(status, expires_at);

CREATE TABLE inventory_reservation_units (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL,
    inventory_unit_id UUID NOT NULL,
    CONSTRAINT uq_reservation_units_pair UNIQUE (reservation_id, inventory_unit_id),
    CONSTRAINT fk_reservation_units_reservation FOREIGN KEY (reservation_id) REFERENCES inventory_reservations(id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_units_unit FOREIGN KEY (inventory_unit_id) REFERENCES inventory_units(id)
);

CREATE INDEX idx_reservation_units_unit ON inventory_reservation_units(inventory_unit_id);

CREATE TABLE inventory_adjustments (
    id UUID PRIMARY KEY,
    sku VARCHAR(80) NOT NULL,
    location_id UUID NOT NULL,
    quantity BIGINT NOT NULL,
    reason VARCHAR(30) NOT NULL,
    reference_id VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_inventory_adjustments_location FOREIGN KEY (location_id) REFERENCES inventory_locations(id),
    CONSTRAINT ck_inventory_adjustments_reason CHECK (reason IN ('PURCHASE_RECEIPT', 'MANUAL_CORRECTION', 'DAMAGE', 'LOSS', 'FOUND', 'RETURN', 'INITIAL_STOCK'))
);

CREATE UNIQUE INDEX uq_inventory_adjustments_reference
    ON inventory_adjustments(sku, location_id, reference_id)
    WHERE reference_id IS NOT NULL;

CREATE TABLE inventory_unit_movements (
    id UUID PRIMARY KEY,
    inventory_unit_id UUID NOT NULL,
    from_location_id UUID,
    to_location_id UUID,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    reference_type VARCHAR(40) NOT NULL,
    reference_id VARCHAR(200),
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_movements_unit FOREIGN KEY (inventory_unit_id) REFERENCES inventory_units(id),
    CONSTRAINT fk_movements_from_location FOREIGN KEY (from_location_id) REFERENCES inventory_locations(id),
    CONSTRAINT fk_movements_to_location FOREIGN KEY (to_location_id) REFERENCES inventory_locations(id)
);

CREATE INDEX idx_inventory_movements_unit_created ON inventory_unit_movements(inventory_unit_id, created_at);
CREATE INDEX idx_inventory_movements_reference ON inventory_unit_movements(reference_type, reference_id);
