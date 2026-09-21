package com.shop.cart.config;

import com.shop.cart.client.CatalogClient;
import com.shop.cart.client.OrderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RemoteClientConfig {
    @Bean
    CatalogClient catalogClient(
            RestClient.Builder builder,
            @Value("${cart.catalog.base-url:http://localhost:8081}") String baseUrl,
            @Value("${cart.catalog.service-token:}") String serviceToken,
            @Value("${cart.catalog.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${cart.catalog.read-timeout-ms:3000}") long readTimeoutMs) {
        return new CatalogClient(restClient(builder, baseUrl, connectTimeoutMs, readTimeoutMs), serviceToken);
    }

    @Bean
    OrderClient orderClient(
            RestClient.Builder builder,
            @Value("${cart.order.base-url:http://localhost:8083}") String baseUrl,
            @Value("${cart.order.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${cart.order.read-timeout-ms:10000}") long readTimeoutMs) {
        return new OrderClient(restClient(builder, baseUrl, connectTimeoutMs, readTimeoutMs));
    }

    private RestClient restClient(RestClient.Builder builder, String baseUrl,
                                  long connectTimeoutMs, long readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return builder.clone().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
