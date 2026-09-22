package com.shop.catalog.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;

@RestController
@Validated
@Tag(name = "Products", description = "Product catalog management")
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @Operation(summary = "Create a product")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDtos.Response create(@Valid @RequestBody ProductDtos.CreateRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Get a product by ID")
    @GetMapping("/{id}")
    public ProductDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @Operation(summary = "Get a product by public slug")
    @GetMapping("/slug/{slug}")
    public ProductDtos.Response getBySlug(@PathVariable String slug) {
        return service.getBySlug(slug);
    }

    @Operation(summary = "Get product discovery facets")
    @GetMapping("/facets")
    public ProductDtos.FacetsResponse facets(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProductStatus status) {
        return service.facets(categoryId, search, status);
    }

    @Operation(summary = "List products", description = "Paginated product listing. Sort format is property,direction (for example name,asc).")
    @GetMapping
    public Page<ProductDtos.Response> list(
            @Parameter(description = "Filter by exact category ID")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Case-insensitive product name search")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by product lifecycle status", example = "ACTIVE")
            @RequestParam(required = false) ProductStatus status,
            @Parameter(description = "Minimum active variant price")
            @RequestParam(required = false) BigDecimal priceMin,
            @Parameter(description = "Maximum active variant price")
            @RequestParam(required = false) BigDecimal priceMax,
            @Parameter(description = "Filter by one or more exact brand names")
            @RequestParam(required = false) List<String> brand,
            @Parameter(description = "Filter by variant attribute as key:value; repeat for multiple values")
            @RequestParam(required = false) List<String> attribute,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, maximum 100", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort expression: property,direction", example = "name,asc")
            @RequestParam(defaultValue = "name,asc") String sort) {
        return service.list(categoryId, search, status, priceMin, priceMax,
                brand == null ? List.of() : brand,
                attribute == null ? List.of() : attribute,
                page, size, sort);
    }

    @Operation(summary = "Update a product")
    @PutMapping("/{id}")
    public ProductDtos.Response update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductDtos.UpdateRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete a product")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
