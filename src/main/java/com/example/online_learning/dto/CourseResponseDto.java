package com.example.online_learning.dto;

import java.util.List;

public record CourseResponseDto(
        Long id,
        String title,
        String level,
        String instructorFirstName,
        String instructorLastName,
        List<LessonResponseDto> lessons,
        List<String> studentNames,
        List<String> categoryNames) {
}
