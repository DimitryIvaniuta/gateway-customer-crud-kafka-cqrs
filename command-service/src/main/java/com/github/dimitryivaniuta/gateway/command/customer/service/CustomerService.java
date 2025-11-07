package com.github.dimitryivaniuta.gateway.command.customer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.command.customer.domain.Customer;
import com.github.dimitryivaniuta.gateway.command.customer.domain.Outbox;
import com.github.dimitryivaniuta.gateway.command.customer.repo.CustomerRepository;
import com.github.dimitryivaniuta.gateway.command.customer.repo.OutboxRepository;
import com.github.dimitryivaniuta.gateway.common.event.CustomerCreated;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customers;

    private final OutboxRepository outbox;

    private final ObjectMapper om;

    @Transactional
    public UUID create(String name, String email, String actor) {
        var id = UUID.randomUUID();
        var entity = Customer.builder().id(id).name(name).email(email).build();
        customers.save(entity);

        var evt = new CustomerCreated(name, email);
        var out = Outbox.builder()
                .aggregateType("Customer").aggregateId(id)
                .eventType("CustomerCreated").version(0)
                .payload(write(evt)).published(false)
                .eventId(UUID.randomUUID())
                .occurredAt(java.time.OffsetDateTime.now())
                .build();
        outbox.save(out);
        return id;
    }

    private String write(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
