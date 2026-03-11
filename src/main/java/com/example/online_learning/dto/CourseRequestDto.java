package com.example.online_learning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CourseRequestDto(
        @NotBlank String title,
        @NotBlank String level,
        @NotBlank String instructorFirstName,
        @NotBlank String instructorLastName,
        @NotBlank String instructorSpecialization,
        @Valid @NotEmpty List<LessonRequestDto> lessons,
        List<Long> studentIds,
        List<String> categoryNames) {
}
