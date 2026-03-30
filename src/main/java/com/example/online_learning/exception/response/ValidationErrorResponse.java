package com.example.online_learning.exception.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Validation error response.")
public record ValidationErrorResponse(
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "Summary message", example = "Validation failed")
        String message,
        @Schema(description = "Error timestamp", example = "2026-03-30T12:00:00")
        LocalDateTime timestamp,
        @Schema(description = "Field-level validation errors")
        Map<String, String> errors
) {
}
