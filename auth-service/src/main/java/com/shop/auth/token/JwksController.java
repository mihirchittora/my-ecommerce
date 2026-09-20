package com.shop.auth.token;

import com.shop.auth.config.SigningKeyService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {
    private final SigningKeyService keys;

    public JwksController(SigningKeyService keys) { this.keys = keys; }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() { return keys.publicJwkSet().toJSONObject(); }
}
