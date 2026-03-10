package com.example.online_learning.mapper;

import com.example.online_learning.dto.StudentResponseDto;
import com.example.online_learning.entity.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    public StudentResponseDto toDto(Student student) {
        return new StudentResponseDto(
                student.getId(),
                student.getFullName(),
                student.getEmail(),
                student.getCourses().stream()
                        .map(course -> course.getTitle())
                        .sorted()
                        .toList());
    }
}
