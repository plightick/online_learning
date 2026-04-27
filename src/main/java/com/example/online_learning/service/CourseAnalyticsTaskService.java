package com.example.online_learning.service;

import com.example.online_learning.dto.AsyncTaskAcceptedResponseDto;
import com.example.online_learning.dto.AsyncTaskState;
import com.example.online_learning.dto.CourseAnalyticsResultDto;
import com.example.online_learning.dto.CourseAnalyticsTaskStatusDto;
import com.example.online_learning.exception.ResourceNotFoundException;
import com.example.online_learning.repository.CourseRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class CourseAnalyticsTaskService {

    private final CourseRepository courseRepository;
    private final CourseAnalyticsAsyncWorker courseAnalyticsAsyncWorker;
    private final AtomicLong taskSequence = new AtomicLong(1);
    private final Map<String, TaskSnapshot> tasks = new ConcurrentHashMap<>();

    public CourseAnalyticsTaskService(
            CourseRepository courseRepository,
            CourseAnalyticsAsyncWorker courseAnalyticsAsyncWorker) {
        this.courseRepository = courseRepository;
        this.courseAnalyticsAsyncWorker = courseAnalyticsAsyncWorker;
    }

    public AsyncTaskAcceptedResponseDto startAnalyticsTask(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course", courseId);
        }

        String taskId = "course-analytics-" + taskSequence.getAndIncrement();
        tasks.put(taskId, TaskSnapshot.pending(taskId, courseId));
        tasks.computeIfPresent(taskId, (ignored, task) -> task.running());

        courseAnalyticsAsyncWorker.buildCourseAnalytics(courseId)
                .whenComplete((result, throwable) -> completeTask(taskId, result, throwable));

        return new AsyncTaskAcceptedResponseDto(taskId, AsyncTaskState.RUNNING);
    }

    public CourseAnalyticsTaskStatusDto getTaskStatus(String taskId) {
        return getTaskSnapshot(taskId).toDto();
    }

    private void completeTask(String taskId, CourseAnalyticsResultDto result, Throwable throwable) {
        if (throwable == null) {
            tasks.computeIfPresent(taskId, (ignored, task) -> task.completed(result));
            return;
        }
        tasks.computeIfPresent(taskId, (ignored, task) -> task.failed(resolveFailureMessage(throwable)));
    }

    private String resolveFailureMessage(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;
        return Optional.ofNullable(cause)
                .map(Throwable::getMessage)
                .orElse("Async task failed");
    }

    private TaskSnapshot getTaskSnapshot(String taskId) {
        TaskSnapshot taskSnapshot = taskId == null ? null : tasks.get(taskId);
        if (taskSnapshot == null) {
            String normalizedTaskId = normalizeTaskId(taskId);
            if (normalizedTaskId != null && !normalizedTaskId.equals(taskId)) {
                taskSnapshot = tasks.get(normalizedTaskId);
            }
        }
        if (taskSnapshot == null) {
            throw new ResourceNotFoundException("Course analytics task '" + taskId + "' was not found");
        }
        return taskSnapshot;
    }

    private String normalizeTaskId(String taskId) {
        if (taskId == null) {
            return null;
        }
        return taskId.trim()
                .replace('\u0441', 'c')
                .replace('\u0421', 'C');
    }

    private record TaskSnapshot(
            String taskId,
            Long courseId,
            AsyncTaskState status,
            LocalDateTime createdAt,
            LocalDateTime completedAt,
            CourseAnalyticsResultDto result,
            String errorMessage) {

        private static TaskSnapshot pending(String taskId, Long courseId) {
            return new TaskSnapshot(
                    taskId,
                    courseId,
                    AsyncTaskState.PENDING,
                    LocalDateTime.now(),
                    null,
                    null,
                    null);
        }

        private TaskSnapshot running() {
            return new TaskSnapshot(taskId, courseId, AsyncTaskState.RUNNING, createdAt, null, null, null);
        }

        private TaskSnapshot completed(CourseAnalyticsResultDto taskResult) {
            return new TaskSnapshot(
                    taskId,
                    courseId,
                    AsyncTaskState.COMPLETED,
                    createdAt,
                    LocalDateTime.now(),
                    taskResult,
                    null);
        }

        private TaskSnapshot failed(String failureMessage) {
            return new TaskSnapshot(
                    taskId,
                    courseId,
                    AsyncTaskState.FAILED,
                    createdAt,
                    LocalDateTime.now(),
                    null,
                    failureMessage);
        }

        private CourseAnalyticsTaskStatusDto toDto() {
            return new CourseAnalyticsTaskStatusDto(
                    taskId,
                    courseId,
                    status,
                    createdAt,
                    completedAt,
                    result,
                    errorMessage);
        }
    }
}
