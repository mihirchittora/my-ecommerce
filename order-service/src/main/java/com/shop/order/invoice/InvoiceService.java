package com.shop.order.invoice;

import com.shop.order.domain.CustomerOrder;
import com.shop.order.domain.OrderRepository;
import com.shop.order.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InvoiceService {
    private final InvoiceRepository invoices;
    private final OrderRepository orders;
    private final InvoicePdfGenerator generator;
    private final InvoiceStorage storage;
    private final InvoiceNumberGenerator numbers;

    public InvoiceService(InvoiceRepository invoices, OrderRepository orders, InvoicePdfGenerator generator,
                          InvoiceStorage storage, InvoiceNumberGenerator numbers) {
        this.invoices = invoices; this.orders = orders; this.generator = generator; this.storage = storage; this.numbers = numbers;
    }

    @Transactional
    public Invoice generateIfAbsent(UUID orderId) {
        Invoice existing = invoices.findByOrderId(orderId).orElse(null);
        if (existing != null) return existing;
        CustomerOrder order = orders.findDetailedById(orderId).orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        Invoice invoice = new Invoice(); invoice.setOrderId(orderId); invoice.setInvoiceNumber(numbers.next());
        invoice.setInvoiceDate(java.time.Instant.now());
        storage.store(invoice, generator.generate(order, invoice));
        return invoices.saveAndFlush(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice get(UUID orderId) { return invoices.findByOrderId(orderId).orElseThrow(() -> new NotFoundException("Invoice is not available for this order")); }

    @Transactional(readOnly = true)
    public byte[] pdf(UUID orderId) { return storage.read(get(orderId)); }
}
