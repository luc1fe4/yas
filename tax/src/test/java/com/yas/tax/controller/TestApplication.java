package com.yas.tax.controller;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

@SpringBootApplication(scanBasePackages = "com.yas.tax.controller", exclude = {
    org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
    org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration.class,
    org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class,
    org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration.class
})
@ImportAutoConfiguration(org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.class)
public class TestApplication {
}
