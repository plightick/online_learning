package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for a course lesson.")
public record LessonRequestDto(
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 100)
        @Schema(description = "Lesson title", example = "Introduction to Java")
        String title,
        @NotNull(message = "must not be null")
        @Min(1)
        @Schema(description = "Lesson duration in minutes", example = "45")
        Integer durationMinutes,
        @NotNull(message = "must not be null")
        @Min(1)
        @Schema(description = "Lesson order inside the course", example = "1")
        Integer lessonOrder) {
}
