package com.example.online_learning.service;

import com.example.online_learning.dto.CourseAnalyticsResultDto;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Lesson;
import com.example.online_learning.exception.ResourceNotFoundException;
import com.example.online_learning.repository.CourseRepository;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CourseAnalyticsAsyncWorker {

    private final CourseRepository courseRepository;

    public CourseAnalyticsAsyncWorker(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Async("courseTaskExecutor")
    public CompletableFuture<CourseAnalyticsResultDto> buildCourseAnalytics(Long courseId) {
        Course course = courseRepository.findDetailedById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        // Small delay makes the in-progress state observable in Swagger and JMeter.
        LockSupport.parkNanos(Duration.ofMillis(250).toNanos());

        List<Lesson> uniqueLessons = uniqueLessons(course.getLessons());
        int totalDurationMinutes = uniqueLessons.stream()
                .mapToInt(Lesson::getDurationMinutes)
                .sum();
        String instructorFullName = course.getInstructor().getFirstName()
                + " "
                + course.getInstructor().getLastName();

        return CompletableFuture.completedFuture(new CourseAnalyticsResultDto(
                course.getId(),
                course.getTitle(),
                instructorFullName,
                uniqueLessons.size(),
                totalDurationMinutes,
                course.getStudents().size(),
                course.getCategories().size()));
    }

    private List<Lesson> uniqueLessons(List<Lesson> lessons) {
        Map<String, Lesson> uniqueLessons = new LinkedHashMap<>();
        for (Lesson lesson : lessons) {
            uniqueLessons.putIfAbsent(lessonKey(lesson), lesson);
        }
        return List.copyOf(uniqueLessons.values());
    }

    private String lessonKey(Lesson lesson) {
        if (lesson.getId() != null) {
            return "id:" + lesson.getId();
        }
        return "order:" + lesson.getLessonOrder() + "|title:" + lesson.getTitle();
    }
}
