package com.github.dimitryivaniuta.gateway.command.customer.repo;

import com.github.dimitryivaniuta.gateway.command.customer.domain.Outbox;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Custom operations for the transactional outbox.
 */
public interface OutboxRepositoryCustom {

    /**
     * Locks and returns up to {@code batchSize} oldest unpublished rows using
     * {@code FOR UPDATE SKIP LOCKED}. Call inside a transactional boundary.
     */
    List<Outbox> lockNextUnpublished(int batchSize);

    /**
     * Marks the given rows as published. Idempotent for empty collections.
     */
    void markPublished(Collection<Long> ids);

    /**
     * Deletes already published rows older than the given timestamp.
     *
     * @return number of rows removed
     */
    int prunePublishedOlderThan(OffsetDateTime threshold);
}
