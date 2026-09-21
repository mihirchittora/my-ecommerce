package com.shop.order.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class IdempotencyFingerprint {
    private IdempotencyFingerprint() {
    }

    public static String sha256(OrderRequestNormalizer.NormalizedRequest request) {
        String canonical = request.currency() + "|" + request.preferredLocationId() + "|"
                + request.lines().stream().map(line -> line.sku() + ":" + line.quantity())
                .reduce((left, right) -> left + ";" + right).orElse("");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
