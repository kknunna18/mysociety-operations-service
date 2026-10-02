package com.mysociety.operations;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtAuthorizationTest {
    @Test
    void permissionsAndRolesBecomeAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(UUID.randomUUID().toString())
                .claim("society_id", UUID.randomUUID().toString()).claim("permissions", List.of("COMPLAINT_VIEW"))
                .claim("roles", List.of("RESIDENT")).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        JwtAuthenticationToken token = (JwtAuthenticationToken) new SecurityConfiguration().jwtAuthenticationConverter().convert(jwt);
        assertThat(token.getAuthorities()).extracting(Object::toString).contains("PERMISSION_COMPLAINT_VIEW", "ROLE_RESIDENT");
    }
}
