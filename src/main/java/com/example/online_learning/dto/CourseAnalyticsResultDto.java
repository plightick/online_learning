package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Calculated analytics for a course.")
public record CourseAnalyticsResultDto(
        @Schema(description = "Course identifier", example = "1")
        Long courseId,
        @Schema(description = "Course title", example = "Spring Boot Intensive")
        String courseTitle,
        @Schema(description = "Instructor full name", example = "Ivan Petrov")
        String instructorFullName,
        @Schema(description = "Total lessons in course", example = "2")
        int lessonCount,
        @Schema(description = "Total duration of all lessons in minutes", example = "105")
        int totalDurationMinutes,
        @Schema(description = "Total enrolled students", example = "2")
        int enrolledStudentCount,
        @Schema(description = "Total assigned categories", example = "2")
        int categoryCount) {
}
