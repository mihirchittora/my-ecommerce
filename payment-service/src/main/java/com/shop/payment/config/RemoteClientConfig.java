package com.shop.payment.config;

import com.shop.payment.order.OrderClient;
import com.shop.payment.order.OrderPaymentNotifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RemoteClientConfig {
    @Bean
    OrderClient orderClient(
            @Value("${app.order.base-url:http://localhost:8083}") String baseUrl,
            @Value("${app.order.service-token:}") String serviceToken,
            @Value("${app.order.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${app.order.read-timeout-ms:3000}") int readTimeoutMs) {
        return new OrderClient(restClient(baseUrl, connectTimeoutMs, readTimeoutMs), serviceToken);
    }

    @Bean
    OrderPaymentNotifier orderPaymentNotifier(
            @Value("${app.order.base-url:http://localhost:8083}") String baseUrl,
            @Value("${app.order.payment-update-path:}") String updatePath,
            @Value("${app.order.service-token:}") String serviceToken,
            @Value("${app.order.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${app.order.read-timeout-ms:3000}") int readTimeoutMs) {
        return new OrderPaymentNotifier(restClient(baseUrl, connectTimeoutMs, readTimeoutMs), updatePath, serviceToken);
    }

    private RestClient restClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
