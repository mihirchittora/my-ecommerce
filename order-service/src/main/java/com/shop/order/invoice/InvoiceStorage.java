package com.shop.order.invoice;

public interface InvoiceStorage {
    void store(Invoice invoice, byte[] pdf);
    byte[] read(Invoice invoice);
}
