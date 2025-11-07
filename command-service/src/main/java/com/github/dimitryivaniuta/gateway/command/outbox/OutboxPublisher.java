package com.github.dimitryivaniuta.gateway.command.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.command.customer.domain.Outbox;
import com.github.dimitryivaniuta.gateway.command.customer.repo.OutboxRepository;
import com.github.dimitryivaniuta.gateway.common.event.CustomerEventEnvelope;
import com.github.dimitryivaniuta.gateway.common.event.CustomerTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxRepository outbox;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper om;

    @Scheduled(fixedDelayString = "1000") // simple poller for demo
    public void publishBatch() {
        List<Outbox> batch = outbox.lockNextUnpublished(200);
        if (batch.isEmpty()) return;

        kafka.executeInTransaction(tpl -> {
            batch.forEach(o -> tpl.send(CustomerTopics.EVENTS, o.getAggregateId().toString(), toEnvelope(o)));
            outbox.markPublished(batch.stream().map(Outbox::getId).toList());
            return null;
        });
    }

    private CustomerEventEnvelope toEnvelope(Outbox o) {
        return CustomerEventEnvelope.builder()
                .eventId(o.getEventId().toString())
                .aggregateId(o.getAggregateId().toString())
                .eventType(o.getEventType())
                .version(o.getVersion())
                .timestampUtcMillis(System.currentTimeMillis())
                .actor("command-service")
                .payload(parse(o.getPayload()))
                .build();
    }

    private Object parse(String json) {
        try {
            return om.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
