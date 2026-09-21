package com.shop.inventory.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
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
    JwtDecoder jwtDecoder(@Value("${auth.issuer:http://localhost:8085}") String issuer,
                          @Value("${auth.audience:ecommerce-api}") String audience,
                          @Value("${auth.jwk-set-uri:http://localhost:8085/.well-known/jwks.json}") String jwkSetUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, new JwtAudienceValidator(audience)));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectProvider<JwtDecoder> decoderProvider,
                                            OrderServiceAuthenticationFilter orderServiceAuthenticationFilter) throws Exception {
        http.csrf(csrf -> csrf.disable()).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (!enabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
        http.addFilterBefore(orderServiceAuthenticationFilter,
                org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter.class);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/transfers").hasAuthority("INVENTORY_TRANSFER")
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory", "/api/v1/inventory/*").hasAuthority("INVENTORY_READ")
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/*/units", "/api/v1/inventory/units/*").hasAuthority("INVENTORY_UNIT_READ")
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/*/receive").hasAuthority("INVENTORY_RECEIVE")
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/*/adjustments").hasAuthority("INVENTORY_ADJUST")
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/adjustments").hasAuthority("INVENTORY_READ")
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/transfers").hasAuthority("INVENTORY_TRANSFER")
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/*/reconcile").hasAuthority("INVENTORY_RECONCILE")
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/*/reservations").hasAnyAuthority("INVENTORY_RESERVE", "SERVICE_ORDER")
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/reservations/*").hasAuthority("INVENTORY_READ")
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/reservations/*/confirm").hasAuthority("INVENTORY_CONFIRM")
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/reservations/*/release").hasAnyAuthority("INVENTORY_RELEASE", "SERVICE_ORDER")
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/reservations/*/cancel").hasAuthority("INVENTORY_RELEASE")
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/locations/**").hasAuthority("INVENTORY_READ")
                .requestMatchers("/api/v1/inventory/locations/**").hasAuthority("INVENTORY_LOCATION_MANAGE")
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
