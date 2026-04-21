package com.example.online_learning.controller;

import com.example.online_learning.controller.api.CourseAnalyticsTaskControllerApi;
import com.example.online_learning.dto.AsyncTaskAcceptedResponseDto;
import com.example.online_learning.dto.CourseAnalyticsTaskStatusDto;
import com.example.online_learning.service.CourseAnalyticsTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class CourseAnalyticsTaskController implements CourseAnalyticsTaskControllerApi {

    private final CourseAnalyticsTaskService courseAnalyticsTaskService;

    public CourseAnalyticsTaskController(CourseAnalyticsTaskService courseAnalyticsTaskService) {
        this.courseAnalyticsTaskService = courseAnalyticsTaskService;
    }

    @Override
    public ResponseEntity<AsyncTaskAcceptedResponseDto> startAnalyticsTask(Long courseId) {
        return ResponseEntity.accepted()
                .body(courseAnalyticsTaskService.startAnalyticsTask(courseId));
    }

    @Override
    public ResponseEntity<CourseAnalyticsTaskStatusDto> getTaskStatus(String taskId) {
        return ResponseEntity.ok(courseAnalyticsTaskService.getTaskStatus(taskId));
    }
}
