package com.github.dimitryivaniuta.gateway.projection;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Projection (read) service:
 * - Consumes events from Kafka.
 * - Projects into read model (schema: read) with idempotent/versioned upserts.
 */
@SpringBootApplication(scanBasePackages = "com.github.dimitryivaniuta.gateway")
@EnableJpaRepositories(basePackages = "com.github.dimitryivaniuta.gateway.projection.repo")
@EntityScan(basePackages = "com.github.dimitryivaniuta.gateway.projection")
public class ProjectionApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ProjectionApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    @Configuration
    static class ProjectionServiceConfig {
        // Place any @Bean overrides here if needed (e.g., custom Kafka listener container factory).
    }
}

