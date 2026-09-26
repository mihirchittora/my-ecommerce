package com.shop.order.returns;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component public class ReturnNumberGenerator { private final JdbcTemplate jdbc; public ReturnNumberGenerator(JdbcTemplate jdbc) { this.jdbc = jdbc; } public String next() { Long value = jdbc.queryForObject("select nextval('return_number_sequence')", Long.class); return "RET-" + String.format("%08d", value == null ? 0 : value); } }
