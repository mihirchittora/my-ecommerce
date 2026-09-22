package com.shop.shipping.carrier;

import com.shop.shipping.common.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class CarrierConfiguration {
    @Bean
    @Primary
    CarrierGateway carrierGateway(SandboxCarrier sandboxCarrier, EasyPostCarrier easyPostCarrier,
                                  @Value("${app.carrier.provider:SANDBOX}") String provider) {
        return switch (provider.trim().toUpperCase()) {
            case "SANDBOX" -> sandboxCarrier;
            case "EASYPOST" -> easyPostCarrier;
            default -> throw new BadRequestException("Unsupported carrier provider=" + provider);
        };
    }

    @Bean
    EasyPostCarrier easyPostCarrier(ObjectMapper objectMapper,
                                    @Value("${app.carrier.easypost.api-base-url:https://api.easypost.com/v2}") String apiBaseUrl,
                                    @Value("${app.carrier.easypost.api-key:}") String apiKey,
                                    @Value("${app.carrier.easypost.webhook-secret:}") String webhookSecret,
                                    @Value("${app.carrier.easypost.origin.name:}") String originName,
                                    @Value("${app.carrier.easypost.origin.phone:}") String originPhone,
                                    @Value("${app.carrier.easypost.origin.line1:}") String originLine1,
                                    @Value("${app.carrier.easypost.origin.city:}") String originCity,
                                    @Value("${app.carrier.easypost.origin.state:}") String originState,
                                    @Value("${app.carrier.easypost.origin.postal-code:}") String originPostalCode,
                                    @Value("${app.carrier.easypost.origin.country:US}") String originCountry,
                                    @Value("${app.carrier.easypost.parcel.length:10}") double parcelLength,
                                    @Value("${app.carrier.easypost.parcel.width:10}") double parcelWidth,
                                    @Value("${app.carrier.easypost.parcel.height:10}") double parcelHeight,
                                    @Value("${app.carrier.easypost.parcel.weight:16}") double parcelWeight,
                                    @Value("${app.carrier.easypost.webhook-timestamp-tolerance-minutes:1}") int tolerance,
                                    @Value("${app.carrier.connect-timeout-ms:2000}") int connectTimeoutMs,
                                    @Value("${app.carrier.read-timeout-ms:5000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return new EasyPostCarrier(RestClient.builder().baseUrl(apiBaseUrl).requestFactory(factory).build(), objectMapper,
                apiKey, webhookSecret, originName, originPhone, originLine1, originCity, originState,
                originPostalCode, originCountry, parcelLength, parcelWidth, parcelHeight, parcelWeight, tolerance);
    }
}
