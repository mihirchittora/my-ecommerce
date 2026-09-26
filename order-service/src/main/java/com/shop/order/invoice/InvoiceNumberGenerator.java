package com.shop.order.invoice;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class InvoiceNumberGenerator {
    private final JdbcTemplate jdbc;
    public InvoiceNumberGenerator(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public String next() { Long value = jdbc.queryForObject("select nextval('invoice_number_sequence')", Long.class); return "INV-" + String.format("%08d", value == null ? 0 : value); }
}
