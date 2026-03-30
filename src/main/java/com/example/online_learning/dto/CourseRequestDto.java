package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Request body for creating or updating a course.")
public record CourseRequestDto(
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 100)
        @Schema(description = "Course title", example = "Java Fundamentals")
        String title,
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 50)
        @Schema(description = "Course level", example = "Beginner")
        String level,
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 50)
        @Schema(description = "Instructor first name", example = "John")
        String instructorFirstName,
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 50)
        @Schema(description = "Instructor last name", example = "Doe")
        String instructorLastName,
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 100)
        @Schema(description = "Instructor specialization", example = "Backend Development")
        String instructorSpecialization,
        @Valid
        @NotEmpty(message = "must not be empty")
        @Schema(description = "Lessons included in the course")
        List<LessonRequestDto> lessons,
        @Schema(description = "Identifiers of enrolled students", example = "[1, 2]")
        List<@Positive(message = "must be greater than 0") Long> studentIds,
        @Schema(description = "Course category names", example = "[\"Programming\", \"Java\"]")
        List<@NotBlank(message = "must not be blank") String> categoryNames) {
}
