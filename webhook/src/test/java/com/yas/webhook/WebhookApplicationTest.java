package com.yas.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
class WebhookApplicationTest {

    @Test
    void contextLoads() {
        // Verify context loads
    }
}
