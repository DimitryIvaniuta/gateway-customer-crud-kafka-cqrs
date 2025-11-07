package com.github.dimitryivaniuta.gateway.common.event;

import lombok.Builder;

@Builder
public record CustomerEventEnvelope(
        String eventId, String aggregateId, String eventType,
        long version, long timestampUtcMillis, String actor,
        Object payload // one of CustomerCreated/Updated/Deleted
) {
}
