package com.example.online_learning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.example.online_learning.dto.CourseAnalyticsResultDto;
import com.example.online_learning.entity.Category;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Instructor;
import com.example.online_learning.entity.Lesson;
import com.example.online_learning.entity.Student;
import com.example.online_learning.exception.ResourceNotFoundException;
import com.example.online_learning.repository.CourseRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseAnalyticsAsyncWorkerTest {

    @Mock
    private CourseRepository courseRepository;

    @Test
    void buildCourseAnalyticsShouldReturnCalculatedSummary() {
        CourseAnalyticsAsyncWorker worker = new CourseAnalyticsAsyncWorker(courseRepository, 0L);
        Course course = detailedCourse(1L);
        when(courseRepository.findDetailedById(1L)).thenReturn(Optional.of(course));

        CourseAnalyticsResultDto result = worker.buildCourseAnalytics(1L).join();

        assertEquals(1L, result.courseId());
        assertEquals("Spring Boot Intensive", result.courseTitle());
        assertEquals("Ivan Petrov", result.instructorFullName());
        assertEquals(2, result.lessonCount());
        assertEquals(105, result.totalDurationMinutes());
        assertEquals(2, result.enrolledStudentCount());
        assertEquals(2, result.categoryCount());
    }

    @Test
    void buildCourseAnalyticsShouldIgnoreDuplicatedLessonsFromFetchJoinResult() {
        CourseAnalyticsAsyncWorker worker = new CourseAnalyticsAsyncWorker(courseRepository, 0L);
        Course course = detailedCourse(2L);
        ReflectionTestUtils.setField(course.getLessons().getFirst(), "id", 11L);
        ReflectionTestUtils.setField(course.getLessons().get(1), "id", 12L);
        course.getLessons().add(course.getLessons().getFirst());
        course.getLessons().add(course.getLessons().get(1));
        when(courseRepository.findDetailedById(2L)).thenReturn(Optional.of(course));

        CourseAnalyticsResultDto result = worker.buildCourseAnalytics(2L).join();

        assertEquals(2, result.lessonCount());
        assertEquals(105, result.totalDurationMinutes());
    }

    @Test
    void buildCourseAnalyticsShouldThrowWhenCourseIsMissing() {
        CourseAnalyticsAsyncWorker worker = new CourseAnalyticsAsyncWorker(courseRepository, 0L);
        when(courseRepository.findDetailedById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> worker.buildCourseAnalytics(99L));

        assertEquals("Course with id 99 was not found", exception.getMessage());
    }

    private static Course detailedCourse(Long id) {
        Course course = new Course("Spring Boot Intensive", "INTERMEDIATE");
        ReflectionTestUtils.setField(course, "id", id);
        course.setInstructor(new Instructor("Ivan", "Petrov", "Java Architecture"));
        course.addLesson(new Lesson("Spring Context", 45, 1));
        course.addLesson(new Lesson("Spring Data JPA", 60, 2));
        course.addStudent(student("Alex", "Novak", "alex@learn.io"));
        course.addStudent(student("Maria", "Green", "maria@learn.io"));
        course.addCategory(new Category("Backend"));
        course.addCategory(new Category("Database"));
        return course;
    }

    private static Student student(String firstName, String lastName, String email) {
        return new Student(firstName, lastName, email);
    }
}
