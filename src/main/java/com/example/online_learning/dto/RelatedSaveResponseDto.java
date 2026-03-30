package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload for transaction persistence demonstrations.")
public record RelatedSaveResponseDto(
        @Schema(description = "Execution mode", example = "WITH_TRANSACTION")
        String mode,
        @Schema(
                description = "Outcome message after simulated failure",
                example = "Simulated failure after partial persistence")
        String message,
        @Schema(description = "Persisted instructor identifier, if any", example = "10")
        Long instructorId,
        @Schema(description = "Persisted course identifier, if any", example = "15")
        Long courseId,
        @Schema(description = "Number of persisted lessons", example = "1")
        long persistedLessons) {
}
