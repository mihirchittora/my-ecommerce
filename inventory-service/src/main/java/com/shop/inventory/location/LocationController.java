package com.shop.inventory.location;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Inventory Locations", description = "Warehouse and stock location management")
@RequestMapping("/api/v1/inventory/locations")
public class LocationController {
    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @Operation(summary = "Create an inventory location")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationDtos.Response create(@Valid @RequestBody LocationDtos.CreateRequest request) {
        return service.create(request);
    }

    @Operation(summary = "List inventory locations")
    @GetMapping
    public List<LocationDtos.Response> list() {
        return service.list();
    }

    @Operation(summary = "Get an inventory location")
    @GetMapping("/{id}")
    public LocationDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @Operation(summary = "Update an inventory location")
    @PutMapping("/{id}")
    public LocationDtos.Response update(@PathVariable UUID id,
                                        @Valid @RequestBody LocationDtos.UpdateRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete an inventory location")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
