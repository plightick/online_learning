package com.example.online_learning.dto;

public record RelatedSaveResponseDto(
        String mode,
        String message,
        Long instructorId,
        Long courseId,
        long persistedLessons) {
}
