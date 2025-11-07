package com.github.dimitryivaniuta.gateway.projection.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dimitryivaniuta.gateway.common.event.CustomerEventEnvelope;
import com.github.dimitryivaniuta.gateway.projection.model.CustomerView;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Read-model projector repository.
 * Applies domain events (create/update/delete) to the denormalized view table.
 * <p>
 * IMPORTANT:
 * - Idempotent by aggregate version: if event.version <= stored.version, it's ignored.
 * - We tolerate out-of-order arrival by ignoring stale versions.
 * - For gaps (version > current+1), we still upsert with the event's data and log a warning;
 * upstream ordering is per-partition, so gaps usually indicate missed older events or a new projector.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomerViewRepository {   // <-- PUBLIC fixes your visibility/compile error

    @PersistenceContext
    private final EntityManager em;

    @Transactional
    public void applyEvent(CustomerEventEnvelope e) {
        final UUID id = UUID.fromString(e.aggregateId());
        CustomerView current = em.find(CustomerView.class, id);
        long currentVersion = current == null ? -1L : current.getVersion();

        // Idempotency: ignore stale/duplicate
        if (e.version() <= currentVersion) {
            if (log.isTraceEnabled()) {
                log.trace("Ignore stale event {} v{} for {} (stored v{})", e.eventType(), e.version(), id, currentVersion);
            }
            return;
        }

        // Optional gap warning
        if (currentVersion >= 0 && e.version() > currentVersion + 1) {
            log.warn("Version gap for {}: incoming v{} > stored v{} + 1 (applying anyway)",
                    id, e.version(), currentVersion);
        }

        switch (e.eventType()) {
            case "CustomerCreated" -> applyCreateOrUpsert(id, e);
            case "CustomerUpdated" -> applyUpdateOrUpsert(id, e);
            case "CustomerDeleted" -> applyDeleteIfExists(current);
            default -> {
                log.warn("Unknown eventType='{}' for aggregate={}; ignoring.", e.eventType(), id);
                return;
            }
        }

        // At this point, the entity state in the persistence context is updated.
        // Flush is handled by transaction boundaries; no explicit em.flush() required.
    }

    /* =========================
       Handlers
       ========================= */

    private void applyCreateOrUpsert(UUID id, CustomerEventEnvelope e) {
        CustomerView cv = em.find(CustomerView.class, id);
        final ObjectNode p = asObjectNode(e.payload());

        final String name = textOrNull(p, "name");
        final String email = textOrNull(p, "email");

        if (cv == null) {
            cv = CustomerView.create(id, nonNull(name, "name", e), nonNull(email, "email", e), e.version());
            em.persist(cv);
        } else {
            // Treat as upsert if projector restarts midway
            boolean changed = cv.applyUpdate(name, email, e.version());
            if (changed) em.merge(cv);
        }
    }

    private void applyUpdateOrUpsert(UUID id, CustomerEventEnvelope e) {
        CustomerView cv = em.find(CustomerView.class, id);
        final ObjectNode p = asObjectNode(e.payload());
        final String name = textOrNull(p, "name");
        final String email = textOrNull(p, "email");

        if (cv == null) {
            // Upsert behavior: create from update if missing (use what we have)
            cv = CustomerView.create(id,
                    nonNullOrFallback(name, "name", "(unknown)", e),
                    nonNullOrFallback(email, "email", "(unknown@invalid)", e),
                    e.version());
            em.persist(cv);
        } else {
            boolean changed = cv.applyUpdate(name, email, e.version());
            if (changed) em.merge(cv);
        }
    }

    private void applyDeleteIfExists(CustomerView current) {
        if (current != null) {
            em.remove(current);
        }
    }

    /* =========================
       JSON helpers
       ========================= */

    private static ObjectNode asObjectNode(Object payload) {
        if (payload instanceof ObjectNode on) return on;
        if (payload instanceof JsonNode jn && jn.isObject()) return (ObjectNode) jn;
        throw new IllegalArgumentException("Payload must be a JSON object; was: " +
                (payload == null ? "null" : payload.getClass().getName()));
    }

    private static String textOrNull(ObjectNode p, String field) {
        JsonNode n = p.get(field);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }

    private static String nonNull(String v, String field, CustomerEventEnvelope e) {
        if (v == null) {
            throw new IllegalArgumentException("Missing required field '" + field +
                    "' in event " + e.eventType() + " for aggregate " + e.aggregateId());
        }
        return v;
    }

    private static String nonNullOrFallback(String v, String field, String fallback, CustomerEventEnvelope e) {
        if (v == null) {
            if (log.isWarnEnabled()) {
                log.warn("Field '{}' missing in {} for {} — using fallback '{}'",
                        field, e.eventType(), e.aggregateId(), fallback);
            }
            return fallback;
        }
        return v;
    }
}
