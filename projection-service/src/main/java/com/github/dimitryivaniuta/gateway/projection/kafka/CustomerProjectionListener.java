package com.github.dimitryivaniuta.gateway.projection.kafka;

import com.github.dimitryivaniuta.gateway.common.event.CustomerEventEnvelope;
import com.github.dimitryivaniuta.gateway.projection.repo.CustomerViewRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.github.dimitryivaniuta.gateway.common.event.CustomerTopics;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes customer domain events and projects them into the read model.
 * Manual ack: we only acknowledge AFTER the DB write succeeds.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerProjectionListener {

    private final CustomerViewRepository repository;

    @KafkaListener(
            topics = CustomerTopics.EVENTS,
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "${spring.kafka.listener.concurrency:1}"
    )
    public void onEvent(
            ConsumerRecord<String, CustomerEventEnvelope> rec,
            Acknowledgment ack
    ) {
        final CustomerEventEnvelope e = rec.value();
        if (e == null) {
            // Deserialization errors should be handled earlier; null payload is a no-op.
            log.warn("Null envelope at topic={} partition={} offset={}", rec.topic(), rec.partition(), rec.offset());
            ack.acknowledge();
            return;
        }

        try {
            repository.applyEvent(e);   // idempotent upsert/remove by version
            ack.acknowledge();          // commit offset ONLY after success
            if (log.isDebugEnabled()) {
                log.debug("Applied {} v{} for aggregate={} at part={} off={}",
                        e.eventType(), e.version(), e.aggregateId(), rec.partition(), rec.offset());
            }
        } catch (Exception ex) {
            // Do NOT ack; let the configured CommonErrorHandler retry / route to DLT.
            log.error("Failed to apply event {} v{} for aggregate={} (part={} off={})",
                    e.eventType(), e.version(), e.aggregateId(), rec.partition(), rec.offset(), ex);
            throw ex;
        }
    }
}
