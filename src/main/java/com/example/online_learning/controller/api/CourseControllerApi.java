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
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

    String BULK_DEMO_REQUEST_EXAMPLE = """
            [
              {
                "title": "Spring Security Deep Dive",
                "level": "ADVANCED",
                "instructorFirstName": "Pavel",
                "instructorLastName": "Ivanov",
                "instructorSpecialization": "Security",
                "lessons": [
                  {
                    "title": "Authentication",
                    "durationMinutes": 40,
                    "lessonOrder": 1
                  }
                ],
                "studentIds": [1],
                "categoryNames": ["Backend", "Security"]
              },
              {
                "title": "Broken Bulk Demo",
                "level": "ADVANCED",
                "instructorFirstName": "Pavel",
                "instructorLastName": "Ivanov",
                "instructorSpecialization": "Security",
                "lessons": [
                  {
                    "title": "Authorization",
                    "durationMinutes": 45,
                    "lessonOrder": 1
                  }
                ],
                "studentIds": [999999],
                "categoryNames": ["Backend"]
              }
            ]
            """;

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
            summary = "Bulk create courses with transaction",
            description = "Saves a list of courses atomically. "
                    + "Use the example with two objects: the first is valid, the "
                    + "second references a missing student and forces rollback of the whole bulk request.")
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
    @PostMapping("/api/courses/bulk/with-transaction")
    ResponseEntity<List<CourseResponseDto>> createCoursesBulkWithTransaction(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Two demo courses where the first is valid and the second is intentionally invalid",
                    required = true,
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = CourseRequestDto.class)),
                            examples = @ExampleObject(
                                    name = "Transactional rollback demo",
                                    summary = "One valid course and one invalid course",
                                    value = BULK_DEMO_REQUEST_EXAMPLE)))
            @Parameter(description = "Course payloads", required = true)
            @Valid @RequestBody List<@Valid CourseRequestDto> requestDtos
    );

    @Operation(
            summary = "Bulk create courses without transaction",
            description = "Saves courses without wrapping the whole request in one transaction. Use the same example: "
                    + "the first course stays in the database, the second fails.")
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
    @PostMapping("/api/courses/bulk/without-transaction")
    ResponseEntity<List<CourseResponseDto>> createCoursesBulkWithoutTransaction(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Two demo courses where the first is valid and the second is intentionally invalid",
                    required = true,
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = CourseRequestDto.class)),
                            examples = @ExampleObject(
                                    name = "Partial save demo",
                                    summary = "The first course is saved, the second one fails",
                                    value = BULK_DEMO_REQUEST_EXAMPLE)))
            @Parameter(description = "Course payloads", required = true)
            @Valid @RequestBody List<@Valid CourseRequestDto> requestDtos
    );

    @Operation(summary = "Get all courses", description = "Returns all courses without filters and pagination.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "All courses retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseResponseDto.class))))
    })
    @GetMapping("/api/courses/all")
    ResponseEntity<List<CourseResponseDto>> getAllCourses();

    @Operation(summary = "Get courses page", description = "Returns a paginated list of courses with optional filters.")
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
