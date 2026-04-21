package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a concurrent increment demo with unsafe and safe counters.")
public record RaceConditionDemoResponseDto(
        @Schema(description = "Number of worker threads used in the demo", example = "64")
        int threads,
        @Schema(description = "How many increments each thread performs", example = "2000")
        int incrementsPerThread,
        @Schema(description = "Expected total after all increments", example = "128000")
        int expectedTotal,
        @Schema(description = "Result of a deliberately unsafe counter", example = "113742")
        int unsafeTotal,
        @Schema(description = "Result of an AtomicInteger-based counter", example = "128000")
        int atomicTotal,
        @Schema(description = "Result of a synchronized counter", example = "128000")
        int synchronizedTotal,
        @Schema(description = "Lost updates in the unsafe scenario", example = "14258")
        int lostUpdates) {
}
