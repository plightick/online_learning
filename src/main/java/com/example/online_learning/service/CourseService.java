package com.example.online_learning.service;

import com.example.online_learning.dto.CourseResponseDto;
import java.util.List;

public interface CourseService {

    CourseResponseDto getCourseById(Long id);

    List<CourseResponseDto> getCourses(String level);
}
