package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Course response payload.")
public record CourseResponseDto(
        @Schema(description = "Course identifier", example = "1")
        Long id,
        @Schema(description = "Course title", example = "Java Fundamentals")
        String title,
        @Schema(description = "Course level", example = "Beginner")
        String level,
        @Schema(description = "Instructor first name", example = "John")
        String instructorFirstName,
        @Schema(description = "Instructor last name", example = "Doe")
        String instructorLastName,
        @Schema(description = "Course lessons")
        List<LessonResponseDto> lessons,
        @Schema(description = "Names of enrolled students")
        List<String> studentNames,
        @Schema(description = "Assigned category names")
        List<String> categoryNames) {
}
