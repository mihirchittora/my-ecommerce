package com.shop.order.invoice;

import org.springframework.stereotype.Component;

@Component
public class DatabaseInvoiceStorage implements InvoiceStorage {
    @Override
    public void store(Invoice invoice, byte[] pdf) { invoice.setPdfBytes(pdf); }

    @Override
    public byte[] read(Invoice invoice) { return invoice.getPdfBytes(); }
}
