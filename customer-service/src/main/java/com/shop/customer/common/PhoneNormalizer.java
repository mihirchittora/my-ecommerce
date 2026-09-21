package com.shop.customer.common;

public final class PhoneNormalizer {
    private PhoneNormalizer() { }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim().replaceAll("[\\s().-]", "");
        if (value.startsWith("00")) value = "+" + value.substring(2);
        if (!value.matches("\\+?[1-9][0-9]{6,14}")) return null;
        return value.startsWith("+") ? value : "+" + value;
    }
}
