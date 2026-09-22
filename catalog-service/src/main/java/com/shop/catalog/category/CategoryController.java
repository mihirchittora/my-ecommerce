package com.shop.catalog.category;

import jakarta.validation.Valid;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Categories", description = "Product category hierarchy management")
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @Operation(summary = "Create a category")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDtos.Response create(@Valid @RequestBody CategoryDtos.CreateRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Get a category by ID")
    @GetMapping("/{id}")
    public CategoryDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @Operation(summary = "Get a category by public slug")
    @GetMapping("/slug/{slug}")
    public CategoryDtos.Response getBySlug(@PathVariable String slug) {
        return service.getBySlug(slug);
    }

    @Operation(summary = "List categories", description = "List root categories or children of a parent category")
    @GetMapping
    public List<CategoryDtos.Response> list(@RequestParam(required = false) UUID parentId) {
        return service.list(parentId);
    }

    @Operation(summary = "Update a category")
    @PutMapping("/{id}")
    public CategoryDtos.Response update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryDtos.UpdateRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete a category")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
