package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after an asynchronous task is accepted.")
public record AsyncTaskAcceptedResponseDto(
        @Schema(description = "Generated task identifier", example = "course-analytics-1")
        String taskId,
        @Schema(description = "Current task status", example = "RUNNING")
        AsyncTaskState status) {
}
