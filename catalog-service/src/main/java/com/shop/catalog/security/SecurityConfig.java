package com.shop.catalog.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${app.security.enabled:true}")
    private boolean enabled;

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
    JwtDecoder jwtDecoder(@Value("${app.auth.issuer:http://localhost:8085}") String issuer,
                          @Value("${app.auth.audience:ecommerce-api}") String audience,
                          @Value("${app.auth.jwk-set-uri:http://localhost:8085/.well-known/jwks.json}") String jwkSetUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, new JwtAudienceValidator(audience)));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            InventoryServiceAuthenticationFilter inventoryServiceAuthenticationFilter) throws Exception {
        http.csrf(csrf -> csrf.disable()).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (!enabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
        http.addFilterBefore(inventoryServiceAuthenticationFilter, BearerTokenAuthenticationFilter.class);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/products/*/reviews/mine").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/products").hasAuthority("PRODUCT_CREATE")
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/*").hasAuthority("PRODUCT_UPDATE")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/*").hasAuthority("PRODUCT_DELETE")
                .requestMatchers(HttpMethod.POST, "/api/v1/categories").hasAuthority("CATEGORY_CREATE")
                .requestMatchers(HttpMethod.PUT, "/api/v1/categories/*").hasAuthority("CATEGORY_UPDATE")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/*").hasAuthority("CATEGORY_DELETE")
                .requestMatchers(HttpMethod.POST, "/api/v1/categories/*/image").hasAuthority("CATEGORY_UPDATE")
                .requestMatchers(HttpMethod.PUT, "/api/v1/categories/*/image").hasAuthority("CATEGORY_UPDATE")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/*/image").hasAuthority("CATEGORY_UPDATE")
                .requestMatchers(HttpMethod.POST, "/api/v1/products/*/images").hasAuthority("PRODUCT_IMAGE_UPLOAD")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/*/images/*").hasAuthority("PRODUCT_IMAGE_DELETE")
                .requestMatchers("/internal/catalog/skus/**").hasAnyAuthority("SERVICE_INVENTORY", "SERVICE_ORDER", "SERVICE_CART")
                .anyRequest().authenticated());
        http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtAuthorityConverter()))
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler()));
        return http.build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> writeError(response, 401, "Authentication is required");
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> writeError(response, 403, "Insufficient permission");
    }

    private void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
