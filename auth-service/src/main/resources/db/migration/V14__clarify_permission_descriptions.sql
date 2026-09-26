UPDATE permissions
SET description = CASE code
    WHEN 'ORDER_READ' THEN 'Read order operations and order history'
    WHEN 'ORDER_CREATE' THEN 'Create orders for customers'
    WHEN 'ORDER_UPDATE' THEN 'Update order details'
    WHEN 'ORDER_CANCEL' THEN 'Cancel customer or operational orders'
    WHEN 'CUSTOMER_READ' THEN 'Read customer profiles'
    WHEN 'CUSTOMER_UPDATE' THEN 'Update customer profiles'
    ELSE description
END
WHERE code IN (
    'ORDER_READ', 'ORDER_CREATE', 'ORDER_UPDATE', 'ORDER_CANCEL',
    'CUSTOMER_READ', 'CUSTOMER_UPDATE'
);
