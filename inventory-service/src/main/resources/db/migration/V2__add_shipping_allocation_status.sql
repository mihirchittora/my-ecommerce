ALTER TABLE inventory_reservations DROP CONSTRAINT ck_inventory_reservations_status;
ALTER TABLE inventory_reservations ADD CONSTRAINT ck_inventory_reservations_status
    CHECK (status IN ('ACTIVE', 'ALLOCATED', 'CONFIRMED', 'RELEASED', 'EXPIRED', 'CANCELLED'));
