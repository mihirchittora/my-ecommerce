package com.shop.catalog;

import com.shop.catalog.category.CategoryRepository;
import com.shop.catalog.category.CategoryStatus;
import com.shop.catalog.product.Product;
import com.shop.catalog.product.ProductRepository;
import com.shop.catalog.variant.ProductVariant;
import com.shop.catalog.variant.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class CatalogIntegrationTest {
    static {
        PortableDockerEnvironment.configure();
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("ecommerce")
            .withUsername("ecommerce")
            .withPassword("ecommerce");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.images.storage-dir", () -> "/tmp/ecommerce-catalog-test-uploads");
    }

    @Autowired MockMvc mvc;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;

    @BeforeEach
    void cleanUploads() throws Exception {
        Path root = Path.of("/tmp/ecommerce-catalog-test-uploads");
        if (Files.exists(root)) {
            try (var paths = Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) { }
                });
            }
        }
    }

    @Test
    void categoryHierarchyAndCycleProtection() throws Exception {
        UUID parent = createCategory("Electronics", null);
        UUID child = createCategory("Mobiles", parent);
        UUID grandchild = createCategory("Smartphones", child);

        mvc.perform(put("/api/v1/categories/{id}", parent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Electronics","parentId":"%s","status":"ACTIVE"}
                                """.formatted(grandchild)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category cannot be moved below one of its descendants"));

        mvc.perform(put("/api/v1/categories/{id}", parent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Electronics","parentId":"%s","status":"ACTIVE"}
                                """.formatted(parent)))
                .andExpect(status().isConflict());
    }

    @Test
    void productCrudAndPaginationSort() throws Exception {
        UUID category = createCategory("Phones", null);
        UUID productId = createProduct(category, "Test Phone", "SKU-1", "INR", "100.00");

        mvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Phone"))
                .andExpect(jsonPath("$.variants", hasSize(1)));

        mvc.perform(put("/api/v1/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(category, "Updated Phone", "SKU-1A", "INR", "125.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Phone"));

        mvc.perform(get("/api/v1/products")
                        .param("categoryId", category.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Updated Phone"));

        mvc.perform(get("/api/v1/products")
                        .param("sort", "string"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort property")));

        mvc.perform(delete("/api/v1/products/{id}", productId))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    void validationRejectsZeroPriceBlankSkuAndUnsupportedCurrency() throws Exception {
        UUID category = createCategory("Validation", null);

        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(category, "Bad Price", "SKU-BAD", "INR", "0.00")))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(category, "Bad SKU", "", "INR", "10.00")))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(category, "Bad Currency", "SKU-CUR", "UMU", "10.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void databaseEnforcesSkuUniqueness() throws Exception {
        UUID category = createCategory("SKU Test", null);
        createProduct(category, "One", "DUP-SKU", "INR", "10.00");

        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(category, "Two", "DUP-SKU", "INR", "20.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("SKU already exists: DUP-SKU"));
    }

    @Test
    void databaseConstraintRejectsDuplicateSkuEvenOutsideApi() {
        var category = new com.shop.catalog.category.Category();
        category.setName("Direct DB");
        category.setSlug("direct-db-" + UUID.randomUUID());
        category.setStatus(CategoryStatus.ACTIVE);
        category = categories.saveAndFlush(category);

        Product first = new Product();
        first.setCategory(category);
        first.setName("First");
        first.setSlug("first-" + UUID.randomUUID());
        first.setStatus(com.shop.catalog.product.ProductStatus.ACTIVE);
        first = products.saveAndFlush(first);

        Product second = new Product();
        second.setCategory(category);
        second.setName("Second");
        second.setSlug("second-" + UUID.randomUUID());
        second.setStatus(com.shop.catalog.product.ProductStatus.ACTIVE);
        second = products.saveAndFlush(second);

        ProductVariant one = new ProductVariant();
        one.setProduct(first);
        one.setSku("DIRECT-DUP");
        one.setPrice(new java.math.BigDecimal("10.00"));
        one.setCurrency("INR");
        one = variants.saveAndFlush(one);

        ProductVariant duplicate = new ProductVariant();
        duplicate.setProduct(second);
        duplicate.setSku("DIRECT-DUP");
        duplicate.setPrice(new java.math.BigDecimal("20.00"));
        duplicate.setCurrency("INR");

        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> variants.saveAndFlush(duplicate));
    }

    @Test
    void imageUploadDownloadAndDelete() throws Exception {
        UUID category = createCategory("Images", null);
        UUID productId = createProduct(category, "Image Phone", "IMG-1", "INR", "100.00");

        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MvcResult upload = mvc.perform(multipart("/api/v1/products/{id}/images", productId)
                        .file(new MockMultipartFile("file", "image.png", "image/png", png))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("sortOrder", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andReturn();

        String response = upload.getResponse().getContentAsString();
        String imageId = response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mvc.perform(get("/api/v1/products/{productId}/images/{imageId}/file", productId, imageId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        mvc.perform(delete("/api/v1/products/{productId}/images/{imageId}", productId, imageId))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/products/{productId}/images/{imageId}/file", productId, imageId))
                .andExpect(status().isNotFound());

        Path uploadRoot = Path.of("/tmp/ecommerce-catalog-test-uploads");
        if (Files.exists(uploadRoot)) {
            try (var paths = Files.walk(uploadRoot)) {
                assertTrue(paths.noneMatch(Files::isRegularFile));
            }
        }
    }

    private UUID createCategory(String name, UUID parentId) throws Exception {
        String body = parentId == null
                ? "{\"name\":\"%s\"}".formatted(name)
                : "{\"name\":\"%s\",\"parentId\":\"%s\"}".formatted(name, parentId);
        MvcResult result = mvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(result.getResponse().getContentAsString().replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1"));
    }

    private UUID createProduct(UUID categoryId, String name, String sku, String currency, String price) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryId, name, sku, currency, price)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(result.getResponse().getContentAsString()
                .replaceFirst("^\\{\\\"id\\\":\\\"([^\\\"]+).*", "$1"));
    }

    private String productJson(UUID categoryId, String name, String sku, String currency, String price) {
        return """
                {
                  "categoryId":"%s",
                  "name":"%s",
                  "description":"Test description",
                  "brand":"Test Brand",
                  "status":"ACTIVE",
                  "variants":[{"sku":"%s","price":%s,"currency":"%s","attributes":{"color":"Black"},"status":"ACTIVE"}],
                  "images":[]
                }
                """.formatted(categoryId, name, sku, price, currency);
    }
}
