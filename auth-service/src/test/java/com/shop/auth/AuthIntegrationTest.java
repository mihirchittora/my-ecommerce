package com.shop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {
    static { PortableDockerEnvironment.configure(); }

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("auth_db").withUsername("auth").withPassword("auth");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("auth.bootstrap.admin-email", () -> "");
        registry.add("auth.bootstrap.admin-password", () -> "");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void registersNormalizesEmailLogsInAndReadsMe() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@test.local";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"StrongPassword123!\",\"firstName\":\"Test\",\"lastName\":\"Customer\"}".formatted(email.toUpperCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));

        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"StrongPassword123!\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode tokens = objectMapper.readTree(login);

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void refreshRotatesAndReusedTokenIsRejected() throws Exception {
        String email = "rotate-" + UUID.randomUUID() + "@test.local";
        register(email);
        JsonNode first = login(email);
        JsonNode second = objectMapper.readTree(mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(first.get("refreshToken").asText())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(first.get("refreshToken").asText())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(second.get("refreshToken").asText())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailAndWeakPasswordAreRejected() throws Exception {
        String email = "duplicate-" + UUID.randomUUID() + "@test.local";
        register(email);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"StrongPassword123!\",\"firstName\":\"Test\",\"lastName\":\"Customer\"}".formatted(email.toUpperCase())))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"weak-%s@test.local\",\"password\":\"short\",\"firstName\":\"Test\",\"lastName\":\"Customer\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("12")));
    }

    @Test
    void invalidPasswordUsesGenericFailureAndTemporarilyLocksAccount() throws Exception {
        String email = "locked-" + UUID.randomUUID() + "@test.local";
        register(email);
        for (int attempt = 0; attempt < 5; attempt++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password."));
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"StrongPassword123!\"}".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void passwordChangeRevokesRefreshAndLogoutRevokesRotatedRefresh() throws Exception {
        String email = "password-" + UUID.randomUUID() + "@test.local";
        register(email);
        JsonNode login = login(email);
        mvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + login.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"StrongPassword123!\",\"newPassword\":\"AnotherStrongPassword123!\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(login.get("refreshToken").asText())))
                .andExpect(status().isUnauthorized());

        JsonNode newLogin = objectMapper.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"AnotherStrongPassword123!\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + newLogin.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(newLogin.get("refreshToken").asText())))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(newLogin.get("refreshToken").asText())))
                .andExpect(status().isUnauthorized());
    }

    private void register(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"StrongPassword123!\",\"firstName\":\"Test\",\"lastName\":\"Customer\"}".formatted(email)))
                .andExpect(status().isCreated());
    }

    private JsonNode login(String email) throws Exception {
        return objectMapper.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"StrongPassword123!\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
}
