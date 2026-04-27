package com.example.online_learning.controller.api;

import com.example.online_learning.dto.AsyncTaskAcceptedResponseDto;
import com.example.online_learning.dto.CourseAnalyticsTaskStatusDto;
import com.example.online_learning.exception.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(
        name = "Course Analytics Tasks",
        description = "Async business operations for course analytics.")
public interface CourseAnalyticsTaskControllerApi {

    @Operation(
            summary = "Start async course analytics",
            description = "Launches asynchronous analytics calculation for an existing course and returns a task ID. "
                    + "Swagger demo request: POST /api/course-analytics/tasks/course/1. "
                    + "Then use returned taskId in GET /api/course-analytics/tasks/{taskId}.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "202",
                description = "Async task accepted",
                content = @Content(
                        schema = @Schema(implementation = AsyncTaskAcceptedResponseDto.class),
                        examples = @ExampleObject(
                                name = "Accepted",
                                value = """
                                        {
                                          "taskId": "course-analytics-1",
                                          "status": "RUNNING"
                                        }
                                        """))),
        @ApiResponse(
                responseCode = "404",
                description = "Course not found",
                content = @Content(
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name = "CourseNotFound",
                                value = """
                                        {
                                          "status": 404,
                                          "message": "Course with id 999 was not found",
                                          "timestamp": "2026-04-27T13:00:00"
                                        }
                                        """)))
    })
    @PostMapping("/api/course-analytics/tasks/course/{courseId}")
    ResponseEntity<AsyncTaskAcceptedResponseDto> startAnalyticsTask(
            @Parameter(description = "Existing course ID", required = true, example = "1")
            @PathVariable @Positive Long courseId
    );

    @Operation(
            summary = "Get async task status",
            description = "Returns current status and result for a previously started course analytics task. "
                    + "Swagger demo request: GET /api/course-analytics/tasks/course-analytics-1. "
                    + "First responses are usually RUNNING, then COMPLETED with analytics payload. "
                    + "Use taskId copied from POST response (do not type manually) to avoid keyboard layout issues.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Task status returned",
                content = @Content(
                        schema = @Schema(implementation = CourseAnalyticsTaskStatusDto.class),
                        examples = {
                            @ExampleObject(
                                    name = "Running",
                                    value = """
                                            {
                                              "taskId": "course-analytics-1",
                                              "courseId": 1,
                                              "status": "RUNNING",
                                              "createdAt": "2026-04-27T13:00:00",
                                              "completedAt": null,
                                              "result": null,
                                              "errorMessage": null
                                            }
                                            """),
                            @ExampleObject(
                                    name = "Completed",
                                    value = """
                                            {
                                              "taskId": "course-analytics-1",
                                              "courseId": 1,
                                              "status": "COMPLETED",
                                              "createdAt": "2026-04-27T13:00:00",
                                              "completedAt": "2026-04-27T13:00:01",
                                              "result": {
                                                "courseId": 1,
                                                "courseTitle": "Spring Boot Intensive",
                                                "instructorFullName": "Ivan Petrov",
                                                "lessonCount": 2,
                                                "totalDurationMinutes": 105,
                                                "enrolledStudentCount": 2,
                                                "categoryCount": 2
                                              },
                                              "errorMessage": null
                                            }
                                            """)
                        })),
        @ApiResponse(
                responseCode = "404",
                description = "Task not found",
                content = @Content(
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name = "TaskNotFound",
                                value = """
                                        {
                                          "status": 404,
                                          "message": "Course analytics task 'missing-task' was not found",
                                          "timestamp": "2026-04-27T13:00:00"
                                        }
                                        """)))
    })
    @GetMapping("/api/course-analytics/tasks/{taskId}")
    ResponseEntity<CourseAnalyticsTaskStatusDto> getTaskStatus(
            @Parameter(description = "Generated task ID", required = true, example = "course-analytics-1")
            @PathVariable String taskId
    );
}
