package com.example.online_learning.service;

import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.dto.CourseSearchQueryType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {

    CourseResponseDto createCourse(CourseRequestDto requestDto);

    List<CourseResponseDto> createCoursesBulkTx(List<CourseRequestDto> requestDtos);

    List<CourseResponseDto> createCoursesBulkNoTx(List<CourseRequestDto> requestDtos);

    CourseResponseDto getCourseById(Long id);

    Page<CourseResponseDto> getCourses(String level, Pageable pageable);

    CourseResponseDto updateCourse(Long id, CourseRequestDto requestDto);

    void deleteCourse(Long id);

    List<CourseResponseDto> getCoursesWithNPlusOne(String level);

    List<CourseResponseDto> getCoursesWithEntityGraph(String level);

    List<CourseResponseDto> searchCourses(
            String categoryName,
            String instructorSpecialization,
            CourseSearchQueryType queryType);

    Page<CourseResponseDto> searchCourses(
            String categoryName,
            String instructorSpecialization,
            CourseSearchQueryType queryType,
            Pageable pageable);
}
