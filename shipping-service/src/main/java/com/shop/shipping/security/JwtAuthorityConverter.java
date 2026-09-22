package com.shop.shipping.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JwtAuthorityConverter implements Converter<Jwt, JwtAuthenticationToken> {
    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        values(jwt.getClaim("permissions")).forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        values(jwt.getClaim("roles")).forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private List<String> values(Object raw) {
        if (raw instanceof Collection<?> collection) return collection.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        return List.of();
    }
}
