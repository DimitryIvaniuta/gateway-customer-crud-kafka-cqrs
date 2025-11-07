package com.github.dimitryivaniuta.gateway.command.customer.repo;

import com.github.dimitryivaniuta.gateway.command.customer.domain.Customer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.UUID;

/**
 * Extra operations optimized for hot paths.
 * - updateIfVersionMatches: single-statement, version-checked update (no entity load)
 */
public interface CustomerRepositoryCustom {

    /**
     * Updates name/email and bumps the version in a single JPQL UPDATE guarded by the expected version.
     *
     * @return true if exactly one row was updated (version matched), false otherwise.
     */
    boolean updateIfVersionMatches(
            @NonNull UUID id,
            long expectedVersion,
            @Nullable String name,
            @Nullable String email
    );

    /**
     * Inserts a new Customer ensuring initial version semantics (helper; delegates to EntityManager persist()).
     */
    Customer insertNew(@NonNull Customer customer);
}
