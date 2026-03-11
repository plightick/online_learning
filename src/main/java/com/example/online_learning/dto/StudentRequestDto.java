package com.example.online_learning.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StudentRequestDto(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email) {
}
