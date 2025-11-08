package com.github.dimitryivaniuta.gateway.command.customer.repo;

import com.github.dimitryivaniuta.gateway.command.customer.domain.Outbox;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.hibernate.jpa.HibernateHints;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
class OutboxRepositoryImpl implements OutboxRepositoryCustom {

    @PersistenceContext
    private final EntityManager em;

    /**
     * Fetches the next batch of unpublished outbox rows, oldest first, and locks them so that
     * concurrent publisher instances do not pick the same rows.
     * <p>
     * Postgres syntax order is: ORDER BY ... LIMIT ... FOR UPDATE SKIP LOCKED
     */
    @Override
    @Transactional
    public List<Outbox> lockNextUnpublished(int batchSize) {
        final String sql = """
                select *
                  from write.outbox
                 where published = false
                 order by occurred_at asc
                 limit :batch
                 for update skip locked
                """;

        @SuppressWarnings("unchecked")
        List<Outbox> rows = em
                .createNativeQuery(sql, Outbox.class)
                .setParameter("batch", Math.max(1, batchSize))
                .setHint(HibernateHints.HINT_READ_ONLY, Boolean.TRUE)
                .getResultList();

        return rows;
    }

    /**
     * Marks selected outbox rows as published. Use only after a successful Kafka transactional send.
     */
    @Override
    @Transactional
    public void markPublished(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return;

        // JPQL bulk update (schema handled by the mapped entity)
        Query q = em.createQuery("""
                update Outbox o
                   set o.published = true
                 where o.id in :ids
                """);
        q.setParameter("ids", ids);
        q.executeUpdate();
        // Clear persistence context so subsequent reads in this Tx don't see stale published flags
        em.clear();
    }

    /**
     * Housekeeping for already-published items (e.g., run daily).
     */
    @Override
    @Transactional
    public int prunePublishedOlderThan(OffsetDateTime threshold) {
        Query q = em.createQuery("""
                delete from Outbox o
                 where o.published = true
                   and o.occurredAt < :ts
                """);
        q.setParameter("ts", threshold);
        int removed = q.executeUpdate();
        em.clear();
        return removed;
    }
}
