package com.yas.backofficebff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration,org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration",
    "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:9999",
    "spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost:9999/auth",
    "spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost:9999/token",
    "spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://localhost:9999/jwks",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class ApplicationTests {

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @Test
    void contextLoads() {
    }
}

