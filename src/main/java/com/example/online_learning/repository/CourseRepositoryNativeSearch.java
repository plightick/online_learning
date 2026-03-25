package com.example.online_learning.repository;

import com.example.online_learning.dto.CourseResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseRepositoryNativeSearch {

    PagedCourseIds findPagedCourseIdsByCategoryAndInstructor(
            String categoryName,
            String instructorSpecialization,
            Pageable pageable);

    Page<CourseResponseDto> searchDetailedCoursesNative(
            String categoryName,
            String instructorSpecialization,
            Pageable pageable);
}
