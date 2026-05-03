package com.yas.sampledata.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @Test
    void jwtAuthenticationConverterForKeycloak_shouldConvertRoles() {
        // Given
        SecurityConfig securityConfig = new SecurityConfig();
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverterForKeycloak();
        Jwt jwt = mock(Jwt.class);
        Map<String, Object> realmAccess = Map.of("roles", Collections.singletonList("ADMIN"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        // When
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) converter.convert(jwt).getAuthorities();

        // Then
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
}
