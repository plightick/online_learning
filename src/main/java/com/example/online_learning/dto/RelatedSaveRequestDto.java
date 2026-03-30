package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Request body for transaction persistence demonstrations.")
public record RelatedSaveRequestDto(
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 50)
        @Schema(description = "Instructor first name", example = "Jane")
        String instructorFirstName,
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 50)
        @Schema(description = "Instructor last name", example = "Brown")
        String instructorLastName,
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 100)
        @Schema(description = "Instructor specialization", example = "Database Design")
        String instructorSpecialization,
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 100)
        @Schema(description = "Course title", example = "SQL Essentials")
        String courseTitle,
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 50)
        @Schema(description = "Course level", example = "Intermediate")
        String courseLevel,
        @Valid
        @NotEmpty(message = "must not be empty")
        @Schema(description = "Lessons that should be persisted")
        List<LessonRequestDto> lessons,
        @Schema(description = "Course category names", example = "[\"Databases\", \"SQL\"]")
        List<@NotBlank(message = "must not be blank") String> categoryNames,
        @Schema(description = "Identifiers of students to attach", example = "[1, 2]")
        List<@Positive(message = "must be greater than 0") Long> studentIds) {
}
