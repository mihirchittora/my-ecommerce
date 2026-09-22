package com.shop.payment.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JwtAuthorityConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(values(jwt.getClaim("permissions")).stream().map(SimpleGrantedAuthority::new).toList());
        authorities.addAll(values(jwt.getClaim("roles")).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private List<String> values(Object raw) {
        if (raw instanceof Collection<?> collection) {
            return collection.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        }
        return List.of();
    }
}
