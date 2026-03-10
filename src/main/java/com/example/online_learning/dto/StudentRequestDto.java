package com.example.online_learning.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StudentRequestDto(@NotBlank String fullName, @NotBlank @Email String email) {
}
