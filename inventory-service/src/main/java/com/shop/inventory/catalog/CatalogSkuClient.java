package com.shop.inventory.catalog;

import com.shop.inventory.common.CatalogUnavailableException;
import com.shop.inventory.common.ConflictException;
import com.shop.inventory.common.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Locale;

@Component
public class CatalogSkuClient implements CatalogSkuLookup {
    private final RestClient client;

    public CatalogSkuClient(RestClient catalogRestClient) {
        this.client = catalogRestClient;
    }

    @Override
    public CatalogSkuResponse requireActive(String rawSku) {
        String sku = normalize(rawSku);
        try {
            CatalogSkuResponse response = client.get()
                    .uri("/internal/catalog/skus/{sku}", sku)
                    .retrieve()
                    .body(CatalogSkuResponse.class);
            if (response == null) {
                throw new CatalogUnavailableException("Catalog returned an empty SKU response", null);
            }
            if (!response.active()) {
                throw new ConflictException("SKU is inactive in Catalog: " + sku);
            }
            return response;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("SKU not found in Catalog: " + sku);
        } catch (CatalogUnavailableException | ConflictException | NotFoundException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new CatalogUnavailableException("Catalog service is unavailable while validating SKU " + sku, ex);
        }
    }

    public String normalize(String rawSku) {
        if (rawSku == null || rawSku.isBlank()) {
            throw new NotFoundException("SKU is required");
        }
        String sku = rawSku.trim().toUpperCase(Locale.ROOT);
        if (!sku.matches("^[A-Z0-9][A-Z0-9._-]{0,79}$")) {
            throw new NotFoundException("SKU not found in Catalog: " + sku);
        }
        return sku;
    }
}
