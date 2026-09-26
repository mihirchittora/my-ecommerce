package com.shop.order.invoice;

import com.shop.order.domain.CustomerOrder;

public interface InvoicePdfGenerator {
    byte[] generate(CustomerOrder order, Invoice invoice);
}
