package com.example.online_learning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RelatedSaveRequestDto(
        @NotBlank String instructorFirstName,
        @NotBlank String instructorLastName,
        @NotBlank String instructorSpecialization,
        @NotBlank String courseTitle,
        @NotBlank String courseLevel,
        @Valid @NotEmpty List<LessonRequestDto> lessons,
        List<String> categoryNames,
        List<Long> studentIds) {
}
