package com.example.online_learning.service;

import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.dto.CourseSearchQueryType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {

    CourseResponseDto createCourse(CourseRequestDto requestDto);

    CourseResponseDto getCourseById(Long id);

    List<CourseResponseDto> getCourses(String level);

    CourseResponseDto updateCourse(Long id, CourseRequestDto requestDto);

    void deleteCourse(Long id);

    List<CourseResponseDto> getCoursesWithNPlusOne(String level);

    List<CourseResponseDto> getCoursesWithEntityGraph(String level);

    Page<CourseResponseDto> searchCourses(
            String categoryName,
            String instructorSpecialization,
            CourseSearchQueryType queryType,
            Pageable pageable);
}
