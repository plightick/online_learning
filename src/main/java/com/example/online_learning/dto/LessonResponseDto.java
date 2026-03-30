package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lesson response payload.")
public record LessonResponseDto(
        @Schema(description = "Lesson identifier", example = "1")
        Long id,
        @Schema(description = "Lesson title", example = "Introduction to Java")
        String title,
        @Schema(description = "Lesson duration in minutes", example = "45")
        Integer durationMinutes,
        @Schema(description = "Lesson order inside the course", example = "1")
        Integer lessonOrder) {
}
