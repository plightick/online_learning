package com.example.online_learning.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.online_learning.dto.AsyncTaskAcceptedResponseDto;
import com.example.online_learning.dto.AsyncTaskState;
import com.example.online_learning.dto.CourseAnalyticsResultDto;
import com.example.online_learning.dto.CourseAnalyticsTaskStatusDto;
import com.example.online_learning.service.CourseAnalyticsTaskService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CourseAnalyticsTaskControllerTest {

    @Mock
    private CourseAnalyticsTaskService courseAnalyticsTaskService;

    private CourseAnalyticsTaskController controller;

    @BeforeEach
    void setUp() {
        controller = new CourseAnalyticsTaskController(courseAnalyticsTaskService);
    }

    @Test
    void startAnalyticsTaskShouldReturnAcceptedResponse() {
        AsyncTaskAcceptedResponseDto responseDto =
                new AsyncTaskAcceptedResponseDto("course-analytics-1", AsyncTaskState.RUNNING);
        when(courseAnalyticsTaskService.startAnalyticsTask(1L)).thenReturn(responseDto);

        ResponseEntity<AsyncTaskAcceptedResponseDto> response = controller.startAnalyticsTask(1L);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertSame(responseDto, response.getBody());
        verify(courseAnalyticsTaskService).startAnalyticsTask(1L);
    }

    @Test
    void getTaskStatusShouldReturnOkResponse() {
        CourseAnalyticsTaskStatusDto responseDto = new CourseAnalyticsTaskStatusDto(
                "course-analytics-1",
                1L,
                AsyncTaskState.COMPLETED,
                LocalDateTime.now().minusSeconds(1),
                LocalDateTime.now(),
                new CourseAnalyticsResultDto(1L, "Spring Boot Intensive", "Ivan Petrov", 2, 105, 2, 2),
                null);
        when(courseAnalyticsTaskService.getTaskStatus("course-analytics-1")).thenReturn(responseDto);

        ResponseEntity<CourseAnalyticsTaskStatusDto> response = controller.getTaskStatus("course-analytics-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseDto, response.getBody());
        verify(courseAnalyticsTaskService).getTaskStatus("course-analytics-1");
    }
}
