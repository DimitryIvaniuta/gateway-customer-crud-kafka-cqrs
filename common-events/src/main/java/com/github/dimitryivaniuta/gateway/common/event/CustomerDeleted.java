package com.github.dimitryivaniuta.gateway.common.event;

import lombok.Builder;

@Builder
public record CustomerDeleted(boolean softDelete) {
}