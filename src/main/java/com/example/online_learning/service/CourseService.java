package com.example.online_learning.service;

import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.CourseResponseDto;
import java.util.List;

public interface CourseService {

    CourseResponseDto createCourse(CourseRequestDto requestDto);

    CourseResponseDto getCourseById(Long id);

    List<CourseResponseDto> getCourses(String level);

    CourseResponseDto updateCourse(Long id, CourseRequestDto requestDto);

    void deleteCourse(Long id);

    List<CourseResponseDto> getCoursesWithNPlusOne(String level);

    List<CourseResponseDto> getCoursesWithEntityGraph(String level);
}
