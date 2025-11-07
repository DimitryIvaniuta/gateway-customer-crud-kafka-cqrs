package com.github.dimitryivaniuta.gateway.common.event;

import lombok.Builder;

@Builder
public record CustomerCreated(String name, String email) {
}