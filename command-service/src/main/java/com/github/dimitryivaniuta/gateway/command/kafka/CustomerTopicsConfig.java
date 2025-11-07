package com.github.dimitryivaniuta.gateway.command.kafka;

import com.github.dimitryivaniuta.gateway.common.event.CustomerTopics;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

@Configuration
public class CustomerTopicsConfig {
    @Bean
    NewTopic customersEvents() {
        return TopicBuilder.name(CustomerTopics.EVENTS)
                .partitions(12).replicas(1) // dev: 1; prod: 3
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, "compact,delete")
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7L * 24 * 60 * 60 * 1000))
                .build();
    }

    @Bean
    NewTopic customersRetry() {
        return TopicBuilder.name(CustomerTopics.RETRY)
                .partitions(12).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(24L * 60 * 60 * 1000))
                .build();
    }

    @Bean
    NewTopic customersDlt() {
        return TopicBuilder.name(CustomerTopics.DLT)
                .partitions(12).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(14L * 24 * 60 * 60 * 1000))
                .build();
    }
}
