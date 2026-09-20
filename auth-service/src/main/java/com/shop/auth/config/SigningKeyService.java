package com.shop.auth.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class SigningKeyService {
    private static final Logger log = LoggerFactory.getLogger(SigningKeyService.class);
    private final AuthProperties properties;
    private RSAKey signingKey;

    public SigningKeyService(AuthProperties properties) {
        this.properties = properties;
    }

    @jakarta.annotation.PostConstruct
    void initialize() {
        try {
            KeyPair pair = loadConfiguredKeyPair();
            if (pair == null) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                pair = generator.generateKeyPair();
                log.warn("JWT key paths are not configured; using an ephemeral development signing key. Configure JWT_PRIVATE_KEY_PATH and JWT_PUBLIC_KEY_PATH outside development.");
            }
            signingKey = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID(properties.getKeyId())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize JWT signing keys", ex);
        }
    }

    private KeyPair loadConfiguredKeyPair() throws Exception {
        String privatePath = properties.getPrivateKeyPath();
        String publicPath = properties.getPublicKeyPath();
        if (privatePath == null || privatePath.isBlank()) {
            if (publicPath != null && !publicPath.isBlank()) {
                throw new IllegalStateException("JWT_PUBLIC_KEY_PATH requires JWT_PRIVATE_KEY_PATH for the auth service");
            }
            return null;
        }
        RSAPrivateKey privateKey = readPrivateKey(Path.of(privatePath));
        RSAPublicKey publicKey = publicPath == null || publicPath.isBlank()
                ? derivePublicKey(privateKey)
                : readPublicKey(Path.of(publicPath));
        return new KeyPair(publicKey, privateKey);
    }

    private RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) {
        if (!(privateKey instanceof RSAPrivateCrtKey crt)) {
            throw new IllegalStateException("A public key file is required when the private key is not an RSA CRT key");
        }
        try {
            var spec = new java.security.spec.RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent());
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to derive RSA public key", ex);
        }
    }

    private RSAPrivateKey readPrivateKey(Path path) throws Exception {
        byte[] der = decodePem(Files.readString(path, StandardCharsets.US_ASCII), "PRIVATE KEY");
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private RSAPublicKey readPublicKey(Path path) throws Exception {
        byte[] der = decodePem(Files.readString(path, StandardCharsets.US_ASCII), "PUBLIC KEY");
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private byte[] decodePem(String pem, String label) {
        String normalized = pem.replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    public RSAKey getSigningKey() { return signingKey; }
    public JWKSet signingJwkSet() { return new JWKSet(signingKey); }
    public JWKSet publicJwkSet() { return new JWKSet(signingKey.toPublicJWK()); }
}
