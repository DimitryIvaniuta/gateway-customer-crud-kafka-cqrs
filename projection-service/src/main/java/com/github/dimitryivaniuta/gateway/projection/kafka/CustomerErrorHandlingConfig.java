package com.github.dimitryivaniuta.gateway.projection.kafka;

import com.github.dimitryivaniuta.gateway.common.event.CustomerTopics;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;

@Configuration
class CustomerErrorHandlingConfig {
    @Bean
    CommonErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        var backoff = new ExponentialBackOffWithMaxRetries(5);
        backoff.setInitialInterval(500);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(10_000);
        var recoverer = new DeadLetterPublishingRecoverer(template, (rec, ex) ->
                new TopicPartition(CustomerTopics.DLT, rec.partition()));
        var handler = new DefaultErrorHandler(recoverer, backoff);
        handler.addNotRetryableExceptions(DeserializationException.class, IllegalArgumentException.class);
        return handler;
    }
}