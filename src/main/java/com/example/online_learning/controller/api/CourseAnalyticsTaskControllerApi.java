package com.example.online_learning.controller.api;

import com.example.online_learning.dto.AsyncTaskAcceptedResponseDto;
import com.example.online_learning.dto.CourseAnalyticsTaskStatusDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
            description = "Launches asynchronous analytics calculation for an existing course and returns a task ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Async task accepted"),
        @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @PostMapping("/api/course-analytics/tasks/course/{courseId}")
    ResponseEntity<AsyncTaskAcceptedResponseDto> startAnalyticsTask(
            @Parameter(description = "Existing course ID", required = true, example = "1")
            @PathVariable @Positive Long courseId
    );

    @Operation(
            summary = "Get async task status",
            description = "Returns current status and result for a previously started course analytics task.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task status returned"),
        @ApiResponse(
                responseCode = "404",
                description = "Task not found",
                useReturnTypeSchema = true)
    })
    @GetMapping("/api/course-analytics/tasks/{taskId}")
    ResponseEntity<CourseAnalyticsTaskStatusDto> getTaskStatus(
            @Parameter(description = "Generated task ID", required = true, example = "course-analytics-1")
            @PathVariable String taskId
    );
}
