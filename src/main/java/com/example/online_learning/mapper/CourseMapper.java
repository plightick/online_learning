package com.example.online_learning.mapper;

import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.dto.LessonResponseDto;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Lesson;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseResponseDto toDto(Course course) {
        return new CourseResponseDto(
                course.getId(),
                course.getTitle(),
                course.getLevel(),
                course.getInstructor().getName(),
                course.getLessons().stream()
                        .sorted((left, right) -> left.getLessonOrder().compareTo(right.getLessonOrder()))
                        .map(this::toLessonDto)
                        .toList(),
                course.getStudents().stream()
                        .map(student -> student.getFullName())
                        .sorted()
                        .toList(),
                course.getCategories().stream()
                        .map(category -> category.getName())
                        .sorted()
                        .toList());
    }

    private LessonResponseDto toLessonDto(Lesson lesson) {
        return new LessonResponseDto(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getDurationMinutes(),
                lesson.getLessonOrder());
    }
}
