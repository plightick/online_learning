package com.example.online_learning.service;

import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.entity.Course;
import com.example.online_learning.exception.CourseNotFoundException;
import com.example.online_learning.mapper.CourseMapper;
import com.example.online_learning.repository.CourseRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    public CourseServiceImpl(CourseRepository courseRepository, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
    }

    @Override
    public CourseResponseDto getCourseById(Long id) {
        Course course = courseRepository
                .findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        return courseMapper.toDto(course);
    }

    @Override
    public List<CourseResponseDto> getCourses(String level) {
        List<Course> courses;
        if (level == null || level.isBlank()) {
            courses = courseRepository.findAll();
        } else {
            courses = courseRepository.findByLevelIgnoreCase(level);
        }
        return courses.stream()
                .map(courseMapper::toDto)
                .toList();
    }
}
