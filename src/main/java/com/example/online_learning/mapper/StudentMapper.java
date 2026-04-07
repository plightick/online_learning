package com.example.online_learning.mapper;

import com.example.online_learning.dto.StudentResponseDto;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    public StudentResponseDto toDto(Student student) {
        return new StudentResponseDto(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getCourses().stream()
                        .map(Course::getTitle)
                        .sorted()
                        .toList());
    }
}
