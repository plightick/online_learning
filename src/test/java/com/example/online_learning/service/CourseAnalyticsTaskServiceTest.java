package com.example.online_learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.online_learning.dto.AsyncTaskAcceptedResponseDto;
import com.example.online_learning.dto.AsyncTaskState;
import com.example.online_learning.dto.CourseAnalyticsResultDto;
import com.example.online_learning.dto.CourseAnalyticsTaskStatusDto;
import com.example.online_learning.exception.ResourceNotFoundException;
import com.example.online_learning.repository.CourseRepository;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseAnalyticsTaskServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseAnalyticsAsyncWorker courseAnalyticsAsyncWorker;

    private CourseAnalyticsTaskService service;

    @BeforeEach
    void setUp() {
        service = new CourseAnalyticsTaskService(courseRepository, courseAnalyticsAsyncWorker);
    }

    @Test
    void startAnalyticsTaskShouldReturnGeneratedTaskIdAndRunningStatus() {
        CompletableFuture<CourseAnalyticsResultDto> future = new CompletableFuture<>();
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(courseAnalyticsAsyncWorker.buildCourseAnalytics(1L)).thenReturn(future);

        AsyncTaskAcceptedResponseDto response = service.startAnalyticsTask(1L);
        CourseAnalyticsTaskStatusDto status = service.getTaskStatus(response.taskId());

        assertEquals("course-analytics-1", response.taskId());
        assertEquals(AsyncTaskState.RUNNING, response.status());
        assertEquals(AsyncTaskState.RUNNING, status.status());
        assertEquals(1L, status.courseId());
        assertNotNull(status.createdAt());
        assertNull(status.completedAt());
        assertNull(status.result());
        assertNull(status.errorMessage());
    }

    @Test
    void startAnalyticsTaskShouldCompleteStatusWhenFutureFinishes() {
        CompletableFuture<CourseAnalyticsResultDto> future = new CompletableFuture<>();
        CourseAnalyticsResultDto result = new CourseAnalyticsResultDto(
                1L,
                "Spring Boot Intensive",
                "Ivan Petrov",
                2,
                105,
                2,
                2);
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(courseAnalyticsAsyncWorker.buildCourseAnalytics(1L)).thenReturn(future);

        AsyncTaskAcceptedResponseDto response = service.startAnalyticsTask(1L);
        future.complete(result);

        CourseAnalyticsTaskStatusDto status = service.getTaskStatus(response.taskId());
        assertEquals(AsyncTaskState.COMPLETED, status.status());
        assertNotNull(status.completedAt());
        assertThat(status.result()).isEqualTo(result);
        assertNull(status.errorMessage());
    }

    @Test
    void startAnalyticsTaskShouldStoreFailureMessageWhenFutureFails() {
        CompletableFuture<CourseAnalyticsResultDto> future = new CompletableFuture<>();
        when(courseRepository.existsById(2L)).thenReturn(true);
        when(courseAnalyticsAsyncWorker.buildCourseAnalytics(2L)).thenReturn(future);

        AsyncTaskAcceptedResponseDto response = service.startAnalyticsTask(2L);
        future.completeExceptionally(new IllegalStateException("direct failure"));

        CourseAnalyticsTaskStatusDto status = service.getTaskStatus(response.taskId());
        assertEquals(AsyncTaskState.FAILED, status.status());
        assertNotNull(status.completedAt());
        assertEquals("direct failure", status.errorMessage());
        assertNull(status.result());
    }

    @Test
    void startAnalyticsTaskShouldUseFallbackMessageWhenFailureCauseIsMissing() {
        CompletableFuture<CourseAnalyticsResultDto> future = new CompletableFuture<>();
        when(courseRepository.existsById(3L)).thenReturn(true);
        when(courseAnalyticsAsyncWorker.buildCourseAnalytics(3L)).thenReturn(future);

        AsyncTaskAcceptedResponseDto response = service.startAnalyticsTask(3L);
        future.completeExceptionally(new CompletionException(null));

        CourseAnalyticsTaskStatusDto status = service.getTaskStatus(response.taskId());
        assertEquals(AsyncTaskState.FAILED, status.status());
        assertEquals("Async task failed", status.errorMessage());
    }

    @Test
    void startAnalyticsTaskShouldRejectMissingCourse() {
        when(courseRepository.existsById(99L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.startAnalyticsTask(99L));

        assertEquals("Course with id 99 was not found", exception.getMessage());
        verify(courseAnalyticsAsyncWorker, never()).buildCourseAnalytics(99L);
    }

    @Test
    void getTaskStatusShouldThrowForUnknownTaskId() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getTaskStatus("missing-task"));

        assertTrue(exception.getMessage().contains("missing-task"));
    }
}
