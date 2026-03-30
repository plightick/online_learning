package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating or updating a student.")
public record StudentRequestDto(
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 50)
        @Schema(description = "Student first name", example = "Alice")
        String firstName,
        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 50)
        @Schema(description = "Student last name", example = "Smith")
        String lastName,
        @NotBlank(message = "must not be blank")
        @Email(message = "must be a well-formed email address")
        @Schema(description = "Student email", example = "alice.smith@example.com")
        String email) {
}
