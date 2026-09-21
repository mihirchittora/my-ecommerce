package com.shop.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class OrderNumberGenerator {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Autowired
    public OrderNumberGenerator(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Clock.systemUTC());
    }

    OrderNumberGenerator(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public String next() {
        Long value = jdbcTemplate.queryForObject("select nextval('order_number_sequence')", Long.class);
        return "ORD-%s-%06d".formatted(
                LocalDate.now(clock.withZone(ZoneOffset.UTC)).format(DateTimeFormatter.BASIC_ISO_DATE), value);
    }
}
