package com.github.dimitryivaniuta.gateway.command.customer.api;

import com.github.dimitryivaniuta.gateway.command.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
class CustomerController {
    private final CustomerService service;

    record CreateReq(String name, String email) {
    }

    record CreateRes(String id) {
    }

    @PostMapping
    public CreateRes create(@RequestBody CreateReq req) {
        var id = service.create(req.name(), req.email(), "api-user");
        return new CreateRes(id.toString());
    }
}
