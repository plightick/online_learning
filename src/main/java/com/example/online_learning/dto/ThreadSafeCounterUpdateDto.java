package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of incrementing a thread-safe counter.")
public record ThreadSafeCounterUpdateDto(
        @Schema(description = "Counter implementation strategy", example = "AtomicInteger")
        String strategy,
        @Schema(description = "Counter value before increment", example = "10")
        int previousValue,
        @Schema(description = "Counter value after increment", example = "15")
        int currentValue,
        @Schema(description = "Applied increment size", example = "5")
        int incrementedBy) {
}
