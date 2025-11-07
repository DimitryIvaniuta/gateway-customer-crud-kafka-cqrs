package com.github.dimitryivaniuta.gateway.projection.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Objects;
import java.util.UUID;

/**
 * Read-model projection of a Customer aggregate.
 * <p>
 * Notes:
 * - The 'version' column stores the aggregate's event/version number (managed by projector),
 * so DO NOT annotate with @Version (that would be managed by JPA and conflict with events).
 * - Schema/table match Flyway migration: schema=read, table=customers_view.
 */
@Entity
@Table(
        name = "customers_view",
        schema = "read",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customers_view_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@DynamicUpdate // update only changed columns
public class CustomerView {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    /**
     * Aggregate version as projected from the latest applied event.
     * Managed by the projector (idempotency check), not by JPA.
     */
    @Column(name = "version", nullable = false)
    private long version;

    /* ----------------------------
       Helpers for projector logic
       ---------------------------- */

    /**
     * Apply an upsert from event data (name/email can be partial for updates).
     * Returns true if any field has changed (useful for conditional writes).
     */
    public boolean applyUpdate(@NullableFields String maybeName, @NullableFields String maybeEmail, long newVersion) {
        boolean changed = false;

        if (maybeName != null && !Objects.equals(this.name, maybeName)) {
            this.name = maybeName;
            changed = true;
        }
        if (maybeEmail != null && !Objects.equals(this.email, maybeEmail)) {
            this.email = maybeEmail;
            changed = true;
        }

        // Always advance stored version to the event's version when projector decided it's the next one.
        if (this.version != newVersion) {
            this.version = newVersion;
            // even if data didn't change (no-op update), version advancement is meaningful
            changed = true;
        }
        return changed;
    }

    /**
     * Convenience factory for a create event.
     */
    public static CustomerView create(UUID id, String name, String email, long version) {
        return CustomerView.builder()
                .id(id)
                .name(name)
                .email(email)
                .version(version)
                .build();
    }

    /* ----------------------------
       Equality by identifier
       ---------------------------- */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerView that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /* -------------------------------------------------
       Minimal nullability marker for method parameters
       ------------------------------------------------- */

    /**
     * Marker to document that a parameter may be null and, if null, should not overwrite the existing value.
     * (Avoids taking a hard javax/jakarta dependency for @Nullable in this module.)
     */
    public @interface NullableFields {
    }
}
