package com.github.dimitryivaniuta.gateway.common.event;

import lombok.Builder;

@Builder
public record CustomerUpdated(String name, String email) {
}