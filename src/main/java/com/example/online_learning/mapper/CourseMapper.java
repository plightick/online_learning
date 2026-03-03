package com.example.online_learning.mapper;

import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.entity.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseResponseDto toDto(Course course) {
        return new CourseResponseDto(
                course.getId(),
                course.getTitle(),
                course.getInstructor(),
                course.getLevel());
    }
}
