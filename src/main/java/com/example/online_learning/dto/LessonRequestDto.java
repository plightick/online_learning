package com.example.online_learning.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record LessonRequestDto(
        @NotBlank String title,
        @Min(1) Integer durationMinutes,
        @Min(1) Integer lessonOrder) {
}
