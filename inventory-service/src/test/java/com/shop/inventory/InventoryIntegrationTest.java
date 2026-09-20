package com.shop.inventory;

import com.shop.inventory.api.InventoryDtos;
import com.shop.inventory.catalog.CatalogSkuLookup;
import com.shop.inventory.catalog.CatalogSkuResponse;
import com.shop.inventory.common.ConflictException;
import com.shop.inventory.movement.MovementRepository;
import com.shop.inventory.reservation.ReservationUnitRepository;
import com.shop.inventory.service.ReservationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class InventoryIntegrationTest {
    static {
        PortableDockerEnvironment.configure();
    }

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("inventory_db")
            .withUsername("inventory")
            .withPassword("inventory");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("inventory.catalog.base-url", () -> "http://catalog-test");
        registry.add("inventory.expiration.fixed-delay-ms", () -> "3600000");
        registry.add("app.security.enabled", () -> "false");
    }

    @Autowired MockMvc mvc;
    @Autowired ReservationService reservationService;
    @Autowired ObjectMapper objectMapper;
    @Autowired ReservationUnitRepository reservationUnits;
    @Autowired MovementRepository movements;

    @MockBean CatalogSkuLookup catalog;

    @BeforeAll
    static void testcontainersAreConfigured() {
        // The static block must execute before Testcontainers creates the PostgreSQL container.
    }

    @org.junit.jupiter.api.BeforeEach
    void mockCatalog() {
        when(catalog.requireActive(anyString())).thenAnswer(invocation -> {
            String sku = invocation.getArgument(0);
            return new CatalogSkuResponse(sku, UUID.randomUUID(), UUID.randomUUID(), "Test Product", true);
        });
    }

    @Test
    void receivesItemizedUnitsIdempotentlyAndReconciles() throws Exception {
        MvcResult summaryBefore = mvc.perform(get("/api/v1/inventory/summary"))
                .andExpect(status().isOk())
                .andReturn();
        var summaryBeforeJson = objectMapper.readTree(summaryBefore.getResponse().getContentAsString());
        long totalBefore = summaryBeforeJson.path("totalInventoryUnits").asLong();
        long availableBefore = summaryBeforeJson.path("availableUnits").asLong();
        long reservedBefore = summaryBeforeJson.path("reservedUnits").asLong();
        long damagedBefore = summaryBeforeJson.path("damagedUnits").asLong();
        long activeLocationsBefore = summaryBeforeJson.path("activeLocations").asLong();

        UUID location = createLocation("LOC-" + shortId(), "Primary");
        String sku = "SKU-" + shortId();
        String reference = "RECEIPT-" + shortId();
        String body = """
                {"locationId":"%s","quantity":2,"referenceId":"%s","units":[
                  {"serialNumber":"SER-%s-1","barcode":"BAR-%s-1"},
                  {"serialNumber":"SER-%s-2","barcode":"BAR-%s-2"}
                ]}
                """.formatted(location, reference, sku, sku, sku, sku);

        mvc.perform(post("/api/v1/inventory/{sku}/receive", sku)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inventory.quantity").value(2))
                .andExpect(jsonPath("$.units", hasSize(2)));

        mvc.perform(post("/api/v1/inventory/{sku}/receive", sku)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inventory.quantity").value(2))
                .andExpect(jsonPath("$.units", hasSize(2)));

        mvc.perform(get("/api/v1/inventory/{sku}/units", sku)
                        .param("locationId", location.toString())
                        .param("sort", "unitCode,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].productName").value("Test Product"));

                mvc.perform(get("/api/v1/inventory")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "sku,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].sku", hasItem(sku)))
                .andExpect(jsonPath("$.content[*].productName", hasItem("Test Product")));

        mvc.perform(get("/api/v1/inventory/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInventoryUnits").value(totalBefore + 2))
                .andExpect(jsonPath("$.availableUnits").value(availableBefore + 2))
                .andExpect(jsonPath("$.reservedUnits").value(reservedBefore))
                .andExpect(jsonPath("$.damagedUnits").value(damagedBefore))
                .andExpect(jsonPath("$.activeLocations").value(activeLocationsBefore + 1));

        mvc.perform(get("/api/v1/inventory/adjustments")
                        .param("sku", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku").value(sku))
                .andExpect(jsonPath("$.content[0].reason").value("PURCHASE_RECEIPT"));

        mvc.perform(post("/api/v1/inventory/{sku}/reconcile", sku)
                        .param("locationId", location.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consistent").value(true));
    }

    @Test
    void listsReservationsWithPaginationAndFilters() throws Exception {
        UUID location = createLocation("LOC-" + shortId(), "Reservation list");
        String sku = "SKU-" + shortId();
        receiveOne(sku, location, "RECEIPT-" + shortId());
        String reference = "RES-" + shortId();

        mvc.perform(post("/api/v1/inventory/{sku}/reservations", sku)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationId\":\"%s\",\"quantity\":1,\"referenceId\":\"%s\"}"
                                .formatted(location, reference)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/inventory/reservations")
                        .param("status", "ACTIVE")
                        .param("sku", sku.toLowerCase())
                        .param("locationId", location.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku").value(sku))
                .andExpect(jsonPath("$.content[0].referenceId").value(reference));
    }

    @Test
    void reservesAndConfirmsUnits() throws Exception {
        UUID location = createLocation("LOC-" + shortId(), "Reserve");
        String sku = "SKU-" + shortId();
        receiveOne(sku, location, "RECEIPT-" + shortId());
        String reservationReference = "RES-" + shortId();
        String reservationBody = """
                {"locationId":"%s","quantity":1,"referenceId":"%s","expiresAt":"%s"}
                """.formatted(location, reservationReference, Instant.now().plusSeconds(600));

        MvcResult result = mvc.perform(post("/api/v1/inventory/{sku}/reservations", sku)
                        .contentType(MediaType.APPLICATION_JSON).content(reservationBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.units", hasSize(1)))
                .andReturn();
        String reservationId = extract(result, "reservationId");
        assertEquals(1, reservationUnits.findByReservation_IdOrderByInventoryUnit_UnitCode(
                UUID.fromString(reservationId)).size());
        assertEquals(1, movements.findByReferenceTypeAndReferenceId(
                "RESERVATION", reservationReference).size());

        mvc.perform(post("/api/v1/inventory/reservations/{id}/confirm", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        assertEquals(2, movements.findByReferenceTypeAndReferenceId(
                "RESERVATION", reservationReference).size());
        mvc.perform(get("/api/v1/inventory/{sku}", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Test Product"))
                .andExpect(jsonPath("$.totalQuantity").value(0));
        mvc.perform(post("/api/v1/inventory/{sku}/reconcile", sku)
                        .param("locationId", location.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consistent").value(true));
    }

    @Test
    void transferAndExplicitNegativeAdjustmentPreserveUnitHistory() throws Exception {
        UUID source = createLocation("LOC-" + shortId(), "Source");
        UUID target = createLocation("LOC-" + shortId(), "Target");
        String sku = "SKU-" + shortId();
        MvcResult received = receiveOne(sku, source, "RECEIPT-" + shortId());
        String unitId = objectMapper.readTree(received.getResponse().getContentAsString())
                .path("units").path(0).path("id").asText();
        String transferReference = "MOVE-" + shortId();

        String transfer = """
                {"sku":"%s","fromLocationId":"%s","toLocationId":"%s","unitIds":["%s"],"referenceId":"%s"}
                """.formatted(sku, source, target, unitId, transferReference);
        mvc.perform(post("/api/v1/inventory/transfers")
                        .contentType(MediaType.APPLICATION_JSON).content(transfer))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/inventory/transfers")
                        .param("sku", sku)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku").value(sku))
                .andExpect(jsonPath("$.content[0].referenceId").value(transferReference))
                .andExpect(jsonPath("$.content[0].unitIds", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fromLocation.id").value(source.toString()))
                .andExpect(jsonPath("$.content[0].toLocation.id").value(target.toString()));

        String adjustment = """
                {"locationId":"%s","quantity":-1,"reason":"LOSS","referenceId":"LOSS-%s","unitIds":["%s"]}
                """.formatted(target, shortId(), unitId);
        mvc.perform(post("/api/v1/inventory/{sku}/adjustments", sku)
                        .contentType(MediaType.APPLICATION_JSON).content(adjustment))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventory.quantity").value(0));
    }

    @Test
    void concurrentReservationsCannotOversellOneUnit() throws Exception {
        UUID location = createLocation("LOC-" + shortId(), "Concurrent");
        String sku = "SKU-" + shortId();
        receiveOne(sku, location, "RECEIPT-" + shortId());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Object> first = () -> tryReserve(sku, location, "CONCURRENT-A-" + shortId());
            Callable<Object> second = () -> tryReserve(sku, location, "CONCURRENT-B-" + shortId());
            List<Future<Object>> results = executor.invokeAll(List.of(first, second));
            long successes = results.stream().filter(future -> {
                try {
                    return !(future.get() instanceof ConflictException);
                } catch (Exception ex) {
                    return false;
                }
            }).count();
            assertEquals(1, successes);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rejectsInvalidReceiveShape() throws Exception {
        UUID location = createLocation("LOC-" + shortId(), "Validation");
        mvc.perform(post("/api/v1/inventory/SKU-INVALID/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationId\":\"%s\",\"quantity\":1,\"referenceId\":\"x\",\"units\":[]}".formatted(location)))
                .andExpect(status().isBadRequest());
    }

    private Object tryReserve(String sku, UUID location, String reference) {
        try {
            return reservationService.reserve(sku, new InventoryDtos.ReservationRequest(
                    location, 1L, reference, Instant.now().plusSeconds(600)));
        } catch (ConflictException ex) {
            return ex;
        }
    }

    private UUID createLocation(String code, String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/inventory/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\",\"name\":\"%s\"}".formatted(code, name)))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(extract(result, "id"));
    }

    private MvcResult receiveOne(String sku, UUID location, String reference) throws Exception {
        return mvc.perform(post("/api/v1/inventory/{sku}/receive", sku)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationId\":\"%s\",\"quantity\":1,\"referenceId\":\"%s\",\"units\":[{\"serialNumber\":\"SER-%s\"}]}".formatted(location, reference, shortId())))
                .andExpect(status().isCreated()).andReturn();
    }

    private String extract(MvcResult result, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path(field).asText();
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
