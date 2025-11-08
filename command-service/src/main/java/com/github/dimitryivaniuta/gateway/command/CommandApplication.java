package com.github.dimitryivaniuta.gateway.command;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Command (write) service:
 * - Exposes REST for CRUD.
 * - Writes OLTP (schema: write) + Outbox row in same TX.
 * - Background publisher (scheduled) drains Outbox -> Kafka (transactional).
 */
@SpringBootApplication(scanBasePackages = "com.github.dimitryivaniuta.gateway")
@EnableJpaRepositories(basePackages = "com.github.dimitryivaniuta.gateway.command.customer.repo")
@EntityScan(basePackages = "com.github.dimitryivaniuta.gateway.command")
@EnableScheduling
public class CommandApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CommandApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    @Configuration
    static class CommandServiceConfig {
        // Place any @Bean overrides here if needed (e.g., TaskScheduler, ObjectMapper customizations).
    }
}
