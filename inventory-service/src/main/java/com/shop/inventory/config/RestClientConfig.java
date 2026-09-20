package com.shop.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient catalogRestClient(
            RestClient.Builder builder,
            @Value("${inventory.catalog.base-url}") String baseUrl,
            @Value("${inventory.catalog.service-token:}") String serviceToken,
            @Value("${inventory.catalog.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${inventory.catalog.read-timeout-ms:3000}") long readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return builder.baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> {
                    if (serviceToken != null && !serviceToken.isBlank()) {
                        headers.set("X-Inventory-Service-Token", serviceToken);
                    }
                })
                .build();
    }
}
