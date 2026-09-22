package com.shop.auth.token;

import com.shop.auth.config.AuthProperties;
import com.shop.auth.user.AppUser;
import com.shop.auth.role.Role;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final AuthProperties properties;

    public JwtTokenService(JwtEncoder encoder, AuthProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public String accessToken(AppUser user, Instant now) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode()).distinct().sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .audience(List.of(properties.getAudience()))
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.getAccessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(properties.getKeyId())
                .type("JWT")
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiresInSeconds() { return properties.getAccessTokenTtl().toSeconds(); }
}
