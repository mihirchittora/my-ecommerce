package com.shop.catalog.review;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.time.Duration;

@Configuration
public class ReviewClientConfig {
    @Bean
    OrderReviewClient orderReviewClient(@Value("${app.order.base-url:http://localhost:8083}") String baseUrl,
                                        @Value("${app.order.connect-timeout-ms:2000}") int connect,
                                        @Value("${app.order.read-timeout-ms:3000}") int read) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connect)); factory.setReadTimeout(Duration.ofMillis(read));
        return new OrderReviewClient(RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build());
    }
}
