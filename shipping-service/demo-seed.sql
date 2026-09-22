-- Local demo fixture for the Shipping/Fulfillment Admin UI.
-- Idempotent: safe to run repeatedly. This does not represent a real shipment workflow.

INSERT INTO fulfillments (
    id, version, order_id, order_number, customer_id, status,
    shipping_address_reference, created_at, updated_at,
    shipping_recipient_name, shipping_phone, shipping_line1, shipping_line2,
    shipping_city, shipping_state, shipping_postal_code, shipping_country, shipping_landmark
) VALUES (
    '11111111-1111-4111-8111-111111111111', 0,
    'e3f0dab2-1542-4a4b-8bff-5f54e7123303', 'ORD-20260922-000004',
    '850348ea-828a-49d1-9ef2-88977ed131ef', 'READY', NULL,
    now() - interval '20 minutes', now() - interval '10 minutes',
    'Demo Recipient', '+919999999999', '1 Demo Street', 'Apartment 4B',
    'Bengaluru', 'Karnataka', '560001', 'IN', 'Near demo warehouse'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO fulfillment_items (
    id, fulfillment_id, order_item_id, sku, product_name_snapshot, quantity,
    reservation_id, inventory_unit_ids, inventory_unit_codes, created_at
) VALUES (
    '22222222-2222-4222-8222-222222222222',
    '11111111-1111-4111-8111-111111111111',
    'a6044a70-8cd3-4dce-9f34-41fdf6bc57d1', 'IP17-BLK-256', 'iPhone 17 Pro', 1,
    'b351cc65-89f3-445d-8af5-aa335b4c5009',
    '["de54c714-440e-43ff-9bb3-61ca15363a0a"]'::jsonb,
    '["UNIT-C5138CF143104307BAE790B50C6C50A5"]'::jsonb,
    now() - interval '20 minutes'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO fulfillment_history (
    id, fulfillment_id, from_status, to_status, event_type, reference_id, notes, created_at
) VALUES
    ('33333333-3333-4333-8333-333333333331', '11111111-1111-4111-8111-111111111111', NULL, 'PENDING', 'FULFILLMENT_CREATED', 'ORD-20260922-000004', 'Demo fulfillment created', now() - interval '20 minutes'),
    ('33333333-3333-4333-8333-333333333332', '11111111-1111-4111-8111-111111111111', 'PENDING', 'READY', 'FULFILLMENT_READY', 'ORD-20260922-000004', 'Demo fulfillment is ready for shipment', now() - interval '19 minutes')
ON CONFLICT (id) DO NOTHING;

INSERT INTO shipments (
    id, version, shipment_number, fulfillment_id, order_id, order_number, customer_id,
    status, carrier, service_level, tracking_number, provider_shipment_id, label_reference,
    shipping_cost, currency, package_count, estimated_delivery_at,
    order_notification_pending, order_notification_last_error,
    created_at, updated_at, shipped_at, delivered_at, cancelled_at
) VALUES (
    '44444444-4444-4444-8444-444444444444', 0, 'SHP-DEMO-000001',
    '11111111-1111-4111-8111-111111111111',
    'e3f0dab2-1542-4a4b-8bff-5f54e7123303', 'ORD-20260922-000004',
    '850348ea-828a-49d1-9ef2-88977ed131ef', 'SHIPPED', 'SANDBOX', 'STANDARD',
    'SBOX-DEMO-001', 'sandbox-shipment-demo-001', 'demo-label-reference',
    0.00, 'INR', 1, now() + interval '3 days', false, NULL,
    now() - interval '15 minutes', now() - interval '5 minutes', now() - interval '5 minutes', NULL, NULL
) ON CONFLICT (id) DO NOTHING;

INSERT INTO shipment_items (
    id, shipment_id, order_item_id, sku, product_name_snapshot, quantity, inventory_unit_id, created_at
) VALUES (
    '55555555-5555-4555-8555-555555555555',
    '44444444-4444-4444-8444-444444444444',
    'a6044a70-8cd3-4dce-9f34-41fdf6bc57d1', 'IP17-BLK-256', 'iPhone 17 Pro', 1,
    'de54c714-440e-43ff-9bb3-61ca15363a0a', now() - interval '15 minutes'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO shipment_history (
    id, shipment_id, from_status, to_status, event_type, carrier_event_id, reference_id, notes, created_at
) VALUES
    ('66666666-6666-4666-8666-666666666661', '44444444-4444-4444-8444-444444444444', NULL, 'CREATED', 'SHIPMENT_CREATED', NULL, 'SHP-DEMO-000001', 'Demo shipment created', now() - interval '15 minutes'),
    ('66666666-6666-4666-8666-666666666662', '44444444-4444-4444-8444-444444444444', 'CREATED', 'SHIPPED', 'SANDBOX_SHIPMENT_CREATED', NULL, 'sandbox-shipment-demo-001', 'Demo carrier shipment created', now() - interval '5 minutes')
ON CONFLICT (id) DO NOTHING;

INSERT INTO shipment_tracking_events (
    id, shipment_id, tracking_number, carrier, event_type, event_status,
    event_location, description, provider_event_id, occurred_at, received_at, created_at
) VALUES (
    '77777777-7777-4777-8777-777777777777',
    '44444444-4444-4444-8444-444444444444', 'SBOX-DEMO-001', 'SANDBOX',
    'PICKED_UP', 'SHIPPED', 'Bengaluru hub', 'Demo parcel picked up by carrier',
    'evt-demo-shipped-001', now() - interval '5 minutes', now() - interval '5 minutes', now() - interval '5 minutes'
) ON CONFLICT (id) DO NOTHING;
