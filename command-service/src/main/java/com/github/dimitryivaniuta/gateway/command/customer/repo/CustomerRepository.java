package com.github.dimitryivaniuta.gateway.command.customer.repo;

import com.github.dimitryivaniuta.gateway.command.customer.domain.Customer;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, CustomerRepositoryCustom {

    // Case-insensitive lookups
    Optional<Customer> findByEmailIgnoreCase(@NonNull String email);

    boolean existsByEmailIgnoreCase(@NonNull String email);

    // Optional: pessimistic lock for special flows (rare; we use optimistic @Version by default)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> lockById(@Param("id") UUID id);
}
