package com.shop.order.invoice;

import com.shop.order.domain.CustomerOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Small dependency-free PDF generator for the local MVP. Replaceable by a full PDF library later. */
@Component
public class SimpleInvoicePdfGenerator implements InvoicePdfGenerator {
    private final String sellerName;
    private final String sellerAddress;

    public SimpleInvoicePdfGenerator(@Value("${app.invoice.seller-name:Morrow Commerce}") String sellerName,
                                     @Value("${app.invoice.seller-address:India}") String sellerAddress) {
        this.sellerName = sellerName;
        this.sellerAddress = sellerAddress;
    }

    @Override
    public byte[] generate(CustomerOrder order, Invoice invoice) {
        List<String> lines = new ArrayList<>();
        lines.add(sellerName + " | " + sellerAddress);
        lines.add("INVOICE " + invoice.getInvoiceNumber());
        lines.add("Order: " + order.getOrderNumber() + "  Date: " + DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(invoice.getInvoiceDate()));
        lines.add("Customer: " + order.getCustomerId());
        if (order.getShippingAddress() != null) {
            lines.add("Ship to: " + order.getShippingAddress().getRecipientName() + ", " + order.getShippingAddress().getLine1()
                    + ", " + order.getShippingAddress().getCity() + " " + order.getShippingAddress().getPostalCode());
        }
        lines.add("Items:");
        order.getItems().forEach(item -> lines.add(item.getProductNameSnapshot() + " | SKU " + item.getSku() + " | qty " + item.getQuantity()
                + " | unit " + item.getUnitPrice() + " | discount " + item.getDiscountAmount() + " | taxable " + item.getTaxableAmount()
                + " | tax " + item.getTaxAmount() + " | line " + item.getSubtotal()));
        lines.add("Subtotal: " + order.getSubtotal());
        lines.add("Discount: " + order.getDiscountAmount());
        lines.add("Shipping: " + order.getShippingAmount());
        lines.add("Tax: " + order.getTaxAmount() + " (rate " + order.getTaxRate() + "%)");
        lines.add("Grand total: " + order.getTotalAmount() + " " + order.getCurrency());
        lines.add("Payment method: " + order.getPaymentMethod() + " | Status: " + order.getStatus());
        return pdf(lines);
    }

    private byte[] pdf(List<String> lines) {
        StringBuilder content = new StringBuilder("BT /F1 9 Tf 40 790 Td 12 TL\n");
        for (String line : lines) content.append("(").append(escape(line)).append(") Tj T*\n");
        content.append("ET");
        byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> objects = List.of(
                bytes("<< /Type /Catalog /Pages 2 0 R >>"),
                bytes("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
                bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"),
                bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"),
                bytes("<< /Length " + stream.length + " >>\nstream\n" + new String(stream, StandardCharsets.ISO_8859_1) + "\nendstream")
        );
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
            List<Integer> offsets = new ArrayList<>();
            for (int index = 0; index < objects.size(); index++) {
                offsets.add(out.size());
                out.write((index + 1 + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
                out.write(objects.get(index)); out.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
            }
            int xref = out.size();
            out.write(("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n").getBytes(StandardCharsets.ISO_8859_1));
            for (int offset : offsets) out.write(String.format("%010d 00000 n \n", offset).getBytes(StandardCharsets.ISO_8859_1));
            out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n").getBytes(StandardCharsets.ISO_8859_1));
            return out.toByteArray();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Could not generate invoice PDF", ex);
        }
    }

    private byte[] bytes(String value) { return value.getBytes(StandardCharsets.ISO_8859_1); }
    private String escape(String value) { return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replaceAll("[^\\x20-\\x7E]", "?"); }
}
