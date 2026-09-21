package com.shop.order.config;

import com.shop.order.client.CatalogClient;
import com.shop.order.client.InventoryClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RemoteClientConfig {
    @Bean
    CatalogClient catalogClient(
            @Value("${app.catalog.base-url:http://localhost:8081}") String baseUrl,
            @Value("${app.catalog.service-token:}") String serviceToken,
            @Value("${app.catalog.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${app.catalog.read-timeout-ms:3000}") int readTimeoutMs) {
        return new CatalogClient(restClient(baseUrl, connectTimeoutMs, readTimeoutMs), serviceToken);
    }

    @Bean
    InventoryClient inventoryClient(
            @Value("${app.inventory.base-url:http://localhost:8082}") String baseUrl,
            @Value("${app.inventory.service-token:}") String serviceToken,
            @Value("${app.inventory.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${app.inventory.read-timeout-ms:3000}") int readTimeoutMs) {
        return new InventoryClient(restClient(baseUrl, connectTimeoutMs, readTimeoutMs), serviceToken);
    }

    private RestClient restClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
