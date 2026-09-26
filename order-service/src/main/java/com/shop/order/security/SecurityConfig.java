package com.shop.order.security;

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
                                            ShippingServiceAuthenticationFilter shippingServiceAuthenticationFilter,
                                            PaymentServiceAuthenticationFilter paymentServiceAuthenticationFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!enabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http.addFilterBefore(shippingServiceAuthenticationFilter,
                org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter.class);
        http.addFilterBefore(paymentServiceAuthenticationFilter,
                org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter.class);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/internal/orders/*/payment-events", "/internal/orders/*/payment-validation").hasAuthority("SERVICE_PAYMENT")
                .requestMatchers("/internal/orders/**").hasAuthority("SERVICE_SHIPPING")
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/my").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/cancel").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/orders").authenticated()
                .requestMatchers("/api/v1/admin/coupons/**").hasAnyAuthority("COUPON_READ", "COUPON_MANAGE")
                .requestMatchers("/api/v1/admin/returns/**").hasAnyAuthority("RETURN_READ", "RETURN_MANAGE")
                .requestMatchers(HttpMethod.GET, "/api/v1/orders").hasAuthority("ORDER_READ")
                .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtAuthorityConverter()))
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
