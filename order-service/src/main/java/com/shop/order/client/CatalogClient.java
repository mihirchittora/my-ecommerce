package com.shop.order.client;

import com.shop.order.exception.InactiveSkuException;
import com.shop.order.exception.MalformedDependencyResponseException;
import com.shop.order.exception.RemoteDependencyException;
import com.shop.order.exception.UnknownSkuException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

public class CatalogClient {
    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);
    private final RestClient client;
    private final String serviceToken;

    public CatalogClient(RestClient client, String serviceToken) {
        this.client = client;
        this.serviceToken = serviceToken;
    }

    public CatalogSku getSellableSku(String rawSku) {
        String sku = rawSku.trim().toUpperCase(java.util.Locale.ROOT);
        try {
            CatalogSku result = client.get().uri("/internal/catalog/skus/{sku}", sku)
                    .headers(headers -> addServiceToken(headers, "X-Order-Service-Token"))
                    .retrieve().body(CatalogSku.class);
            validate(result, sku);
            if (!result.active()) throw new InactiveSkuException(sku);
            return result;
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.value() == 404) throw new UnknownSkuException(sku);
            log.warn("Catalog lookup failed for sku={} status={}", sku, status.value());
            throw new RemoteDependencyException("Catalog", "lookup failed with HTTP " + status.value(), ex);
        } catch (ResourceAccessException ex) {
            log.warn("Catalog lookup unavailable for sku={}", sku);
            throw new RemoteDependencyException("Catalog", "lookup timed out or could not connect", ex);
        } catch (RestClientException ex) {
            log.warn("Catalog returned a malformed response for sku={}", sku);
            throw new MalformedDependencyResponseException("Catalog", "SKU response could not be decoded");
        }
    }

    private void validate(CatalogSku result, String requestedSku) {
        if (result == null || result.sku() == null || result.productName() == null
                || result.price() == null || result.currency() == null
                || result.price().compareTo(BigDecimal.ZERO) < 0
                || result.attributes() == null) {
            throw new MalformedDependencyResponseException("Catalog", "SKU response for " + requestedSku + " is incomplete");
        }
        if (!requestedSku.equals(result.sku().trim().toUpperCase(java.util.Locale.ROOT))) {
            throw new MalformedDependencyResponseException("Catalog", "SKU response does not match the requested SKU");
        }
    }

    private void addServiceToken(org.springframework.http.HttpHeaders headers, String header) {
        if (serviceToken != null && !serviceToken.isBlank()) headers.set(header, serviceToken);
    }
}
