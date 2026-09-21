package com.shop.customer;

import com.shop.customer.address.AddressDtos;
import com.shop.customer.address.AddressType;
import com.shop.customer.address.CustomerAddress;
import com.shop.customer.address.CustomerAddressRepository;
import com.shop.customer.address.AddressService;
import com.shop.customer.common.BadRequestException;
import com.shop.customer.common.ForbiddenException;
import com.shop.customer.common.NotFoundException;
import com.shop.customer.customer.Customer;
import com.shop.customer.customer.CustomerDtos;
import com.shop.customer.customer.CustomerRepository;
import com.shop.customer.customer.CustomerService;
import com.shop.customer.customer.CustomerAdminService;
import com.shop.customer.customer.CustomerStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CustomerServiceIntegrationTest {
    static {
        PortableDockerEnvironment.configure();
    }

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("customer_db")
            .withUsername("customer")
            .withPassword("customer");

    @BeforeAll
    static void beforeAll() {
        // The explicit method keeps this test usable with Colima and Docker Desktop.
        PortableDockerEnvironment.configure();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CustomerService customers;
    @Autowired CustomerAdminService customerAdmin;
    @Autowired AddressService addresses;
    @Autowired CustomerRepository customerRepository;
    @Autowired CustomerAddressRepository addressRepository;
    @Autowired MockMvc mvc;

    @Test
    void createsOneProfilePerAuthIdentityAndOnlyMutableFieldsChange() {
        UUID authUserId = UUID.randomUUID();
        Authentication authentication = auth(authUserId);

        Customer first = customers.getOrCreateActive(authentication);
        Customer second = customers.getOrCreateActive(authentication);
        Customer updated = customers.updateProfile(authentication,
                new CustomerDtos.ProfilePatchRequest(" Mihir ", "Chittora", "9876543210"));

        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(updated.getAuthUserId()).isEqualTo(authUserId);
        assertThat(updated.getFirstName()).isEqualTo("Mihir");
        assertThat(updated.getPhone()).isEqualTo("+9876543210");
        assertThat(customerRepository.findByAuthUserId(authUserId).orElseThrow().getId()).isEqualTo(first.getId());
    }

    @Test
    void concurrentFirstProfileRequestsRemainIdempotent() throws Exception {
        UUID authUserId = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> profileAfter(start, authUserId));
            var second = executor.submit(() -> profileAfter(start, authUserId));
            start.countDown();
            assertThat(first.get().getId()).isEqualTo(second.get().getId());
        } finally {
            executor.shutdownNow();
        }
        assertThat(customerRepository.findByAuthUserId(authUserId)).isPresent();
    }

    @Test
    void databaseRejectsDuplicateAuthUserId() {
        UUID authUserId = UUID.randomUUID();
        customers.getOrCreateActive(auth(authUserId));
        Customer duplicate = new Customer();
        duplicate.setAuthUserId(authUserId);
        duplicate.setStatus(CustomerStatus.ACTIVE);

        assertThatThrownBy(() -> customerRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void addressOwnershipAndShippingBillingDefaultsAreEnforced() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        customers.getOrCreateActive(auth(ownerId));
        customers.getOrCreateActive(auth(otherId));

        CustomerAddress shippingA = addresses.create(auth(ownerId), request(AddressType.SHIPPING, false));
        CustomerAddress shippingB = addresses.create(auth(ownerId), request(AddressType.SHIPPING, false));
        CustomerAddress billing = addresses.create(auth(ownerId), request(AddressType.BILLING, false));
        addresses.setDefault(auth(ownerId), shippingB.getId());

        List<CustomerAddress> ownerAddresses = addressRepository.findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(
                shippingA.getCustomer().getId());
        assertThat(ownerAddresses).filteredOn(CustomerAddress::isDefaultAddress)
                .extracting(CustomerAddress::getAddressType)
                .containsExactlyInAnyOrder(AddressType.SHIPPING, AddressType.BILLING);
        assertThat(ownerAddresses).filteredOn(address -> address.getAddressType() == AddressType.SHIPPING)
                .filteredOn(CustomerAddress::isDefaultAddress).hasSize(1)
                .first().extracting(CustomerAddress::getId).isEqualTo(shippingB.getId());
        assertThat(billing.isDefaultAddress()).isTrue();

        assertThatThrownBy(() -> addresses.get(auth(otherId), shippingA.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> addresses.update(auth(otherId), shippingA.getId(), request(AddressType.SHIPPING, true)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> addresses.delete(auth(otherId), shippingA.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> addresses.setDefault(auth(otherId), shippingA.getId()))
                .isInstanceOf(NotFoundException.class);

        AddressDtos.AddressRequest replacement = new AddressDtos.AddressRequest(AddressType.SHIPPING,
                "Updated Recipient", "+91 90000-00000", "456 New Street", null, "Jaipur",
                "Rajasthan", "302001", "IN", null, false);
        CustomerAddress updated = addresses.update(auth(ownerId), shippingA.getId(), replacement);
        assertThat(updated.getLine1()).isEqualTo("456 New Street");
        addresses.delete(auth(ownerId), updated.getId());
        assertThat(addressRepository.findById(shippingB.getId()).orElseThrow().isDefaultAddress()).isTrue();
    }

    @Test
    void concurrentDefaultChangesLeaveExactlyOneDefault() throws Exception {
        UUID authUserId = UUID.randomUUID();
        customers.getOrCreateActive(auth(authUserId));
        CustomerAddress first = addresses.create(auth(authUserId), request(AddressType.SHIPPING, false));
        CustomerAddress second = addresses.create(auth(authUserId), request(AddressType.SHIPPING, false));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> setAfter(start, authUserId, first.getId()));
            var b = executor.submit(() -> setAfter(start, authUserId, second.getId()));
            start.countDown();
            a.get();
            b.get();
        } finally {
            executor.shutdownNow();
        }

        List<CustomerAddress> shipping = addressRepository.findByCustomerIdAndAddressTypeOrderByDefaultAddressDescCreatedAtDesc(
                first.getCustomer().getId(), AddressType.SHIPPING);
        assertThat(shipping).filteredOn(CustomerAddress::isDefaultAddress).hasSize(1);
    }

    @Test
    void inactiveCustomerCannotUseCustomerOperations() {
        UUID authUserId = UUID.randomUUID();
        Customer customer = customers.getOrCreateActive(auth(authUserId));
        customer.setStatus(CustomerStatus.INACTIVE);
        customerRepository.saveAndFlush(customer);

        assertThatThrownBy(() -> customers.getOrCreateActive(auth(authUserId)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectsInvalidPhoneValues() {
        UUID authUserId = UUID.randomUUID();
        customers.getOrCreateActive(auth(authUserId));
        assertThatThrownBy(() -> customers.updateProfile(auth(authUserId),
                new CustomerDtos.ProfilePatchRequest(null, null, "not-a-phone")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> addresses.create(auth(authUserId),
                requestWithPhone("invalid")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void adminCanSearchCustomerProfilesAndReadAddresses() {
        UUID authUserId = UUID.randomUUID();
        Customer customer = customers.getOrCreateActive(auth(authUserId));
        customers.updateProfile(auth(authUserId), new CustomerDtos.ProfilePatchRequest("Mihir", "Operations", "+919876543210"));
        addresses.create(auth(authUserId), request(AddressType.SHIPPING, false));

        var page = customerAdmin.list("operations", CustomerStatus.ACTIVE, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(CustomerDtos.CustomerResponse::id).contains(customer.getId());
        assertThat(customerAdmin.get(customer.getId()).firstName()).isEqualTo("Mihir");
        assertThat(customerAdmin.addresses(customer.getId())).hasSize(1)
                .first().extracting(AddressDtos.AddressResponse::addressType).isEqualTo(AddressType.SHIPPING);
    }

    @Test
    void adminCanListCustomersWithoutSearchOrStatusFilters() {
        customers.getOrCreateActive(auth(UUID.randomUUID()));

        var page = customerAdmin.list(null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    void unauthenticatedProfileRequestIsRejected() throws Exception {
        mvc.perform(get("/api/v1/customers/me"))
                .andExpect(status().isUnauthorized());
    }

    private void setAfter(CountDownLatch start, UUID authUserId, UUID addressId) {
        try {
            start.await();
            addresses.setDefault(auth(authUserId), addressId);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private Customer profileAfter(CountDownLatch start, UUID authUserId) {
        try {
            start.await();
            return customers.getOrCreateActive(auth(authUserId));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private Authentication auth(UUID authUserId) {
        return new TestingAuthenticationToken(authUserId.toString(), "test", List.of());
    }

    private AddressDtos.AddressRequest request(AddressType type, boolean isDefault) {
        return new AddressDtos.AddressRequest(type, "Mihir Chittora", "+91 98765-43210",
                "123 Example Street", "Apartment 4B", "Udaipur", "Rajasthan", "313001", "IN",
                "Near the market", isDefault);
    }

    private AddressDtos.AddressRequest requestWithPhone(String phone) {
        return new AddressDtos.AddressRequest(AddressType.SHIPPING, "Mihir Chittora", phone,
                "123 Example Street", null, "Udaipur", "Rajasthan", "313001", "IN", null, false);
    }
}
