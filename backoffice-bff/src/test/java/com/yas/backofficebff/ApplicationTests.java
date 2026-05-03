package com.yas.backofficebff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "spring.autoconfigure.exclude=org.springdoc.webmvc.ui.autoconfigure.SwaggerUiAutoConfiguration"
})
@ActiveProfiles("test")
class ApplicationTests {

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private SecurityWebFilterChain securityWebFilterChain;

    @Test
    void contextLoads() {
    }
}

