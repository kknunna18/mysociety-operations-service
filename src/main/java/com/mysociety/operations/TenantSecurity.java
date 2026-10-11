package com.mysociety.operations;

import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

record TenantContext(UUID userId, UUID societyId, Set<String> roles, Set<String> permissions, String correlationId) {
    static TenantContext from(Jwt jwt) {
        try {
            UUID user = UUID.fromString(jwt.getSubject());
            UUID society = UUID.fromString(jwt.getClaimAsString("society_id"));
            return new TenantContext(user, society, claims(jwt, "roles"), claims(jwt, "permissions"), CorrelationIds.current());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JwtValidationException("Required tenant claims are invalid",
                    List.of(new OAuth2Error("invalid_token", "sub and society_id must be UUIDs", null)));
        }
    }

    private static Set<String> claims(Jwt jwt, String claim) {
        List<String> values = jwt.getClaimAsStringList(claim);
        return values == null ? Set.of() : Set.copyOf(values);
    }
}

final class TenantContexts {
    private TenantContexts() {
    }

    static TenantContext current() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken token))
            throw new IllegalStateException("JWT authentication is required");
        return TenantContext.from(token.getToken());
    }
}

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration {
    @Bean
    JwtDecoder jwtDecoder(@Value("${operations.security.jwt-hs256-secret}") String secret) {
        if (secret == null || secret.length() < 32)
            throw new IllegalStateException("JWT_HS256_SECRET must be at least 32 characters");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("mysociety-identity"));
        return decoder;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, Converter<Jwt, AbstractAuthenticationToken> converter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health/**", "/actuator/prometheus",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll().anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return new Converter<Jwt, AbstractAuthenticationToken>() {
            @Override
            public AbstractAuthenticationToken convert(Jwt jwt) {
                TenantContext context = TenantContext.from(jwt);
                var authorities = new ArrayList<org.springframework.security.core.GrantedAuthority>();
                context.permissions().forEach(p -> authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_" + p)));
                context.roles().forEach(r -> authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + r)));
                return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
            }
        };
    }
}
