package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Status of an asynchronous course analytics task.")
public record CourseAnalyticsTaskStatusDto(
        @Schema(description = "Task identifier", example = "course-analytics-1")
        String taskId,
        @Schema(description = "Course identifier", example = "1")
        Long courseId,
        @Schema(description = "Current task state", example = "COMPLETED")
        AsyncTaskState status,
        @Schema(description = "Task creation timestamp")
        LocalDateTime createdAt,
        @Schema(description = "Task completion timestamp")
        LocalDateTime completedAt,
        @Schema(description = "Result of completed analytics task")
        CourseAnalyticsResultDto result,
        @Schema(
                description = "Failure details if the task finished with error",
                example = "Course with id 99 was not found")
        String errorMessage) {
}
