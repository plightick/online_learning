package com.example.online_learning.controller;

import com.example.online_learning.controller.api.CourseControllerApi;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CourseController implements CourseControllerApi {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/api/courses")
    public ResponseEntity<CourseResponseDto> createCourse(@Valid @RequestBody CourseRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.createCourse(requestDto));
    }

    @GetMapping("/api/courses")
    public ResponseEntity<Page<CourseResponseDto>> getCourses(
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending) {
        Pageable pageable = createSortedPageable(page, size, sortBy, ascending);
        return ResponseEntity.ok(courseService.getCourses(level, pageable));
    }

    @GetMapping("/api/courses/search")
    public ResponseEntity<Page<CourseResponseDto>> searchCourses(
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String instructorSpecialization,
            @RequestParam(defaultValue = "JPQL") CourseSearchQueryType queryType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = createPageable(page, size);
        return ResponseEntity.ok(courseService.searchCourses(
                categoryName,
                instructorSpecialization,
                queryType,
                pageable));
    }

    @GetMapping("/api/courses/{id:\\d+}")
    public ResponseEntity<CourseResponseDto> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PutMapping("/api/courses/{id:\\d+}")
    public ResponseEntity<CourseResponseDto> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequestDto requestDto) {
        return ResponseEntity.ok(courseService.updateCourse(id, requestDto));
    }

    @DeleteMapping("/api/courses/{id:\\d+}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/courses/n-plus-one")
    public ResponseEntity<List<CourseResponseDto>> demonstrateNPlusOne(
            @RequestParam(required = false) String level) {
        return ResponseEntity.ok(courseService.getCoursesWithNPlusOne(level));
    }

    @GetMapping("/api/courses/optimized")
    public ResponseEntity<List<CourseResponseDto>> getCoursesWithEntityGraph(
            @RequestParam(required = false) String level) {
        return ResponseEntity.ok(courseService.getCoursesWithEntityGraph(level));
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
