package com.example.online_learning.controller.api;

import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.dto.CourseSearchQueryType;
import com.example.online_learning.exception.response.ErrorResponse;
import com.example.online_learning.exception.response.ValidationErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Course Controller", description = "Course management and search endpoints")
public interface CourseControllerApi {

    @Operation(summary = "Create course", description = "Creates a new course with lessons and optional relations.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "201",
                description = "Course created successfully",
                content = @Content(schema = @Schema(implementation = CourseResponseDto.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Request conflicts with existing data",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/courses")
    ResponseEntity<CourseResponseDto> createCourse(
            @Parameter(description = "Course payload", required = true)
            @Valid @RequestBody CourseRequestDto requestDto
    );

    @Operation(
            summary = "Bulk create courses",
            description = "Imports a list of courses and can demonstrate atomic or partial persistence.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "201",
                description = "Courses created successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseResponseDto.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body or empty list",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/courses/bulk")
    ResponseEntity<List<CourseResponseDto>> createCoursesBulk(
            @Parameter(description = "Course payloads", required = true)
            @Valid @RequestBody List<@Valid CourseRequestDto> requestDtos,
            @Parameter(
                    description = "When true the whole bulk request is atomic; when false successful items stay saved",
                    example = "true")
            @RequestParam(defaultValue = "true") boolean transactional
    );

    @Operation(summary = "Get courses", description = "Returns a paginated list of courses.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Courses retrieved successfully"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid filter or paging parameters",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/courses")
    ResponseEntity<Page<CourseResponseDto>> getCourses(
            @Parameter(description = "Course level filter", example = "Beginner")
            @RequestParam(required = false) String level,
            @Parameter(description = "Requested page number, starting from 1", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction", example = "true")
            @RequestParam(defaultValue = "true") boolean ascending
    );

    @Operation(summary = "Search courses", description = "Searches courses by category and instructor filters.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Courses found successfully"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid search parameters",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/courses/search")
    ResponseEntity<Page<CourseResponseDto>> searchCourses(
            @Parameter(description = "Category name filter", example = "Programming")
            @RequestParam(required = false) String categoryName,
            @Parameter(description = "Instructor specialization filter", example = "Backend Development")
            @RequestParam(required = false) String instructorSpecialization,
            @Parameter(description = "Query implementation type", example = "JPQL")
            @RequestParam(defaultValue = "JPQL") CourseSearchQueryType queryType,
            @Parameter(description = "Requested page number, starting from 1", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size
    );

    @Operation(summary = "Get course by ID", description = "Returns detailed course information by identifier.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Course found",
                content = @Content(schema = @Schema(implementation = CourseResponseDto.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid course identifier",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Course not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/courses/{id}")
    ResponseEntity<CourseResponseDto> getCourseById(
            @Parameter(description = "Course ID", required = true, example = "1")
            @PathVariable @Positive Long id
    );

    @Operation(summary = "Update course", description = "Updates an existing course.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Course updated successfully",
                content = @Content(schema = @Schema(implementation = CourseResponseDto.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid course identifier or request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Course or student not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/api/courses/{id}")
    ResponseEntity<CourseResponseDto> updateCourse(
            @Parameter(description = "Course ID", required = true, example = "1")
            @PathVariable @Positive Long id,
            @Parameter(description = "Updated course payload", required = true)
            @Valid @RequestBody CourseRequestDto requestDto
    );

    @Operation(summary = "Delete course", description = "Deletes a course by ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Course deleted successfully"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid course identifier",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Course not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/courses/{id}")
    ResponseEntity<Void> deleteCourse(
            @Parameter(description = "Course ID", required = true, example = "1")
            @PathVariable @Positive Long id
    );

    @Operation(summary = "Demonstrate N+1", description = "Returns courses using a non-optimized loading strategy.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Courses retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseResponseDto.class))))
    })
    @GetMapping("/api/courses/n-plus-one")
    ResponseEntity<List<CourseResponseDto>> demonstrateNPlusOne(
            @Parameter(description = "Course level filter", example = "Intermediate")
            @RequestParam(required = false) String level
    );

    @Operation(summary = "Get optimized courses", description = "Returns courses using the optimized entity graph.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Courses retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseResponseDto.class))))
    })
    @GetMapping("/api/courses/optimized")
    ResponseEntity<List<CourseResponseDto>> getCoursesWithEntityGraph(
            @Parameter(description = "Course level filter", example = "Advanced")
            @RequestParam(required = false) String level
    );
}
