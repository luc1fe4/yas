package com.yas.tax.controller;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

@SpringBootApplication(scanBasePackages = "com.yas.tax.controller", exclude = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@ImportAutoConfiguration(org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class)
public class TestApplication {
}
