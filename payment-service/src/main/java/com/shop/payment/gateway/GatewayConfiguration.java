package com.shop.payment.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayConfiguration {
    @Bean
    SandboxGateway sandboxGateway(GatewayProperties properties, ObjectMapper objectMapper) {
        return new SandboxGateway(properties.getSandbox().getWebhookSecret(), objectMapper);
    }

    @Bean
    GatewayRegistry gatewayRegistry(List<PaymentGateway> gateways) {
        Map<GatewayProvider, PaymentGateway> byProvider = new EnumMap<>(GatewayProvider.class);
        gateways.forEach(gateway -> byProvider.put(gateway.provider(), gateway));
        return new GatewayRegistry(byProvider);
    }
}
