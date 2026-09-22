package com.shop.payment.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.gateway")
public class GatewayProperties {
    private GatewayProvider defaultProvider = GatewayProvider.SANDBOX;
    private final Sandbox sandbox = new Sandbox();

    public GatewayProvider getDefaultProvider() { return defaultProvider; }
    public void setDefaultProvider(GatewayProvider defaultProvider) { this.defaultProvider = defaultProvider; }
    public Sandbox getSandbox() { return sandbox; }

    public static class Sandbox {
        private String webhookSecret = "dev-sandbox-webhook-secret";

        public String getWebhookSecret() { return webhookSecret; }
        public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    }
}
