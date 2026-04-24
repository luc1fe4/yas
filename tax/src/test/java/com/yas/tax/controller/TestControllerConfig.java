package com.yas.tax.controller;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

@SpringBootApplication(scanBasePackages = "com.yas.tax.controller")
@ImportAutoConfiguration(org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.class)
public class TestControllerConfig {
}
