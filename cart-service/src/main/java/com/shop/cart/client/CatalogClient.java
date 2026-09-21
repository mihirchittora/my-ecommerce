package com.shop.cart.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Locale;

public class CatalogClient {
    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);
    private final RestClient client;
    private final String serviceToken;

    public CatalogClient(RestClient client, String serviceToken) {
        this.client = client;
        this.serviceToken = serviceToken;
    }

    public CatalogSku getSku(String rawSku) {
        String sku = normalize(rawSku);
        try {
            CatalogSku response = client.get()
                    .uri("/internal/catalog/skus/{sku}", sku)
                    .headers(headers -> addServiceToken(headers))
                    .retrieve()
                    .body(CatalogSku.class);
            validate(response, sku);
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new UnknownSkuException(sku);
            log.warn("Catalog lookup failed sku={} status={}", sku, ex.getStatusCode().value());
            throw new CartDependencyException("Catalog", "lookup failed with HTTP " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            log.warn("Catalog lookup unavailable sku={}", sku);
            throw new CartDependencyException("Catalog", "lookup timed out or could not connect", ex);
        } catch (RestClientException ex) {
            log.warn("Catalog returned a malformed response sku={}", sku);
            throw new CartDependencyException("Catalog", "returned a malformed SKU response", ex);
        }
    }

    public CatalogSku requireActive(String rawSku) {
        CatalogSku result = getSku(rawSku);
        if (!result.active()) throw new InactiveSkuException(result.sku());
        return result;
    }

    private void validate(CatalogSku response, String requestedSku) {
        if (response == null || response.sku() == null || response.productName() == null
                || response.price() == null || response.currency() == null
                || response.price().compareTo(BigDecimal.ZERO) < 0 || response.attributes() == null) {
            throw new CartDependencyException("Catalog", "returned an incomplete SKU response for " + requestedSku);
        }
        if (!requestedSku.equals(response.sku().trim().toUpperCase(Locale.ROOT))) {
            throw new CartDependencyException("Catalog", "returned a different SKU than requested");
        }
    }

    private String normalize(String rawSku) {
        if (rawSku == null || rawSku.isBlank()) {
            throw new CartApiException("SKU is required");
        }
        String sku = rawSku.trim().toUpperCase(Locale.ROOT);
        if (!sku.matches("^[A-Z0-9][A-Z0-9._-]{0,79}$")) {
            throw new CartApiException("SKU format is invalid");
        }
        return sku;
    }

    private void addServiceToken(HttpHeaders headers) {
        if (serviceToken != null && !serviceToken.isBlank()) {
            headers.set("X-Cart-Service-Token", serviceToken);
        }
    }

    private static class CartApiException extends com.shop.cart.service.CartApiException {
        private CartApiException(String message) {
            super(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_SKU", message);
        }
    }
}
