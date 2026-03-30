package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Student response payload.")
public record StudentResponseDto(
        @Schema(description = "Student identifier", example = "1")
        Long id,
        @Schema(description = "Student first name", example = "Alice")
        String firstName,
        @Schema(description = "Student last name", example = "Smith")
        String lastName,
        @Schema(description = "Student email", example = "alice.smith@example.com")
        String email,
        @Schema(description = "Titles of enrolled courses")
        List<String> enrolledCourses) {
}
