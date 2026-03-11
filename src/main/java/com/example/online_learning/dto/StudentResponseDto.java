package com.example.online_learning.dto;

import java.util.List;

public record StudentResponseDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        List<String> enrolledCourses) {
}
