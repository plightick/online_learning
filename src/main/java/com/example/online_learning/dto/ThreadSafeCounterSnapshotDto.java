package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Snapshot of a thread-safe counter value.")
public record ThreadSafeCounterSnapshotDto(
        @Schema(description = "Counter implementation strategy", example = "AtomicInteger")
        String strategy,
        @Schema(description = "Current counter value", example = "15")
        int value) {
}
