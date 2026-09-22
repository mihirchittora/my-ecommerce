package com.shop.shipping.shipment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ShipmentNumberGenerator {
    private final JdbcTemplate jdbc;

    public ShipmentNumberGenerator(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public String next() {
        Long sequence = jdbc.queryForObject("select nextval('shipment_number_sequence')", Long.class);
        return "SHP-" + LocalDate.now() + "-" + String.format("%06d", sequence == null ? 0 : sequence);
    }
}
