package com.shop.payment.gateway;

import com.shop.payment.common.BadRequestException;

import java.util.Map;

public class GatewayRegistry {
    private final Map<GatewayProvider, PaymentGateway> gateways;

    public GatewayRegistry(Map<GatewayProvider, PaymentGateway> gateways) {
        this.gateways = Map.copyOf(gateways);
    }

    public PaymentGateway get(GatewayProvider provider) {
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null) throw new BadRequestException("Payment provider is not configured: " + provider);
        return gateway;
    }
}
