package com.example.online_learning.controller;

import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.dto.CourseSearchQueryType;
import com.example.online_learning.service.CourseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponseDto createCourse(@Valid @RequestBody CourseRequestDto requestDto) {
        return courseService.createCourse(requestDto);
    }

    @GetMapping
    public Page<CourseResponseDto> getCourses(
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending) {
        Pageable pageable = createSortedPageable(page, size, sortBy, ascending);
        return courseService.getCourses(level, pageable);
    }

    @GetMapping("/search")
    public Page<CourseResponseDto> searchCourses(
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String instructorSpecialization,
            @RequestParam(defaultValue = "JPQL") CourseSearchQueryType queryType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = createPageable(page, size);
        return courseService.searchCourses(
                categoryName,
                instructorSpecialization,
                queryType,
                pageable);
    }

    @GetMapping("/{id:\\d+}")
    public CourseResponseDto getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id);
    }

    @PutMapping("/{id:\\d+}")
    public CourseResponseDto updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequestDto requestDto) {
        return courseService.updateCourse(id, requestDto);
    }

    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
    }

    @GetMapping("/n-plus-one")
    public List<CourseResponseDto> demonstrateNPlusOne(@RequestParam(required = false) String level) {
        return courseService.getCoursesWithNPlusOne(level);
    }

    @GetMapping("/optimized")
    public List<CourseResponseDto> getCoursesWithEntityGraph(@RequestParam(required = false) String level) {
        return courseService.getCoursesWithEntityGraph(level);
    }

    private Pageable createSortedPageable(int page, int size, String sortBy, boolean ascending) {
        int normalizedPage = Math.max(page - 1, 0);
        int normalizedSize = Math.max(size, 1);
        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return PageRequest.of(normalizedPage, normalizedSize, sort);
    }

    private Pageable createPageable(int page, int size) {
        int normalizedPage = Math.max(page - 1, 0);
        int normalizedSize = Math.max(size, 1);
        return PageRequest.of(normalizedPage, normalizedSize);
    }
}
