package com.shop.order.api;

import com.shop.order.domain.OrderRepository;
import com.shop.order.exception.NotFoundException;
import com.shop.order.invoice.Invoice;
import com.shop.order.invoice.InvoiceService;
import com.shop.order.service.OrderApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {
    private final InvoiceService invoices;
    private final OrderRepository orders;

    public InvoiceController(InvoiceService invoices, OrderRepository orders) { this.invoices = invoices; this.orders = orders; }

    @GetMapping("/api/v1/orders/{orderId}/invoice")
    public ResponseEntity<byte[]> customerInvoice(@PathVariable UUID orderId, Authentication authentication) {
        var order = orders.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        if (!order.getCustomerId().equals(authentication.getName())) throw new NotFoundException("Invoice not found");
        return download(orderId);
    }

    @GetMapping("/api/v1/admin/orders/{orderId}/invoice")
    @PreAuthorize("hasAuthority('ORDER_READ')")
    public ResponseEntity<byte[]> adminInvoice(@PathVariable UUID orderId) { return download(orderId); }

    private ResponseEntity<byte[]> download(UUID orderId) {
        Invoice invoice = invoices.generateIfAbsent(orderId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(invoice.getInvoiceNumber() + ".pdf").build().toString())
                .body(invoices.pdf(orderId));
    }
}
