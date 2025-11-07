package com.github.dimitryivaniuta.gateway.command.customer.repo;

import com.github.dimitryivaniuta.gateway.command.customer.domain.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, Long>, OutboxRepositoryCustom {
}
