package com.example.online_learning.dto;

import java.util.List;

public record CourseResponseDto(
        Long id,
        String title,
        String level,
        String instructorName,
        List<LessonResponseDto> lessons,
        List<String> studentNames,
        List<String> categoryNames) {
}
