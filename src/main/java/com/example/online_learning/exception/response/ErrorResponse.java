package com.example.online_learning.exception.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Standard error response.")
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "404")
        int status,
        @Schema(description = "Error message", example = "Course with id 10 was not found")
        String message,
        @Schema(description = "Error timestamp", example = "2026-03-30T12:00:00")
        LocalDateTime timestamp
) {
}
