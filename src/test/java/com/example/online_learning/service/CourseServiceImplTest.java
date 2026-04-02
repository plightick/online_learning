package com.example.online_learning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.online_learning.cache.CourseSearchCache;
import com.example.online_learning.cache.CourseSearchCacheInvalidator;
import com.example.online_learning.dto.CourseRequestDto;
import com.example.online_learning.dto.CourseResponseDto;
import com.example.online_learning.dto.LessonRequestDto;
import com.example.online_learning.entity.Category;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Instructor;
import com.example.online_learning.entity.Student;
import com.example.online_learning.exception.BadRequestException;
import com.example.online_learning.exception.ResourceNotFoundException;
import com.example.online_learning.mapper.CourseMapper;
import com.example.online_learning.repository.CategoryRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.repository.StudentRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CourseSearchCache courseSearchCache;

    @Mock
    private CourseSearchCacheInvalidator courseSearchCacheInvalidator;

    private CourseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourseServiceImpl(
                courseRepository,
                instructorRepository,
                studentRepository,
                categoryRepository,
                new CourseMapper(),
                courseSearchCache,
                courseSearchCacheInvalidator);
    }

    @Test
    void createCoursesBulkTxShouldCreateAllCoursesAndInvalidateCacheOnce() {
        Student student = student(1L, "Anna", "Petrova", "anna@example.com");
        Instructor instructor = instructor("Pavel", "Ivanov", "Security");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Pavel", "Ivanov"))
                .thenReturn(Optional.of(instructor));
        when(categoryRepository.findByNameIgnoreCase(anyString()))
                .thenAnswer(invocation -> Optional.of(new Category(invocation.getArgument(0))));

        AtomicLong ids = new AtomicLong(10L);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            ReflectionTestUtils.setField(course, "id", ids.getAndIncrement());
            return course;
        });

        List<CourseResponseDto> responseDtos = service.createCoursesBulkTx(List.of(
                courseRequest("Spring Security Deep Dive", List.of(1L)),
                courseRequest("Docker for Java Teams", List.of(1L))));

        assertEquals(2, responseDtos.size());
        assertEquals(10L, responseDtos.getFirst().id());
        assertEquals(11L, responseDtos.get(1).id());
        assertEquals("Spring Security Deep Dive", responseDtos.getFirst().title());
        assertEquals(1, responseDtos.getFirst().studentNames().size());
        assertEquals("Anna Petrova", responseDtos.getFirst().studentNames().getFirst());
        verify(courseRepository, times(2)).save(any(Course.class));
        verify(courseSearchCacheInvalidator).invalidate();
    }

    @Test
    void createCoursesBulkTxShouldRejectEmptyRequest() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createCoursesBulkTx(List.of()));

        assertTrue(exception.getMessage().contains("at least one item"));
        verify(courseRepository, never()).save(any(Course.class));
        verify(courseSearchCacheInvalidator, never()).invalidate();
    }

    @Test
    void createCoursesBulkNoTxShouldStopOnMissingStudentAfterFirstSuccessfulCourse() {
        Student student = student(1L, "Anna", "Petrova", "anna@example.com");
        Instructor instructor = instructor("Pavel", "Ivanov", "Security");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());
        when(instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Pavel", "Ivanov"))
                .thenReturn(Optional.of(instructor));
        when(categoryRepository.findByNameIgnoreCase("Backend"))
                .thenReturn(Optional.of(new Category("Backend")));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            ReflectionTestUtils.setField(course, "id", 50L);
            return course;
        });

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.createCoursesBulkNoTx(List.of(
                        courseRequest("Spring Security Deep Dive", List.of(1L)),
                        courseRequest("Broken Bulk Demo", List.of(999L)))));

        assertTrue(exception.getMessage().contains("999"));
        verify(courseRepository, times(1)).save(any(Course.class));
        verify(instructorRepository, times(1))
                .findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Pavel", "Ivanov");
        verify(categoryRepository, times(1)).findByNameIgnoreCase("Backend");
        verify(courseSearchCacheInvalidator).invalidate();
    }

    private static CourseRequestDto courseRequest(String title, List<Long> studentIds) {
        return new CourseRequestDto(
                title,
                "ADVANCED",
                "Pavel",
                "Ivanov",
                "Security",
                List.of(new LessonRequestDto("Authentication", 40, 1)),
                studentIds,
                List.of("Backend"));
    }

    private static Student student(Long id, String firstName, String lastName, String email) {
        Student student = new Student(firstName, lastName, email);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    private static Instructor instructor(String firstName, String lastName, String specialization) {
        return new Instructor(firstName, lastName, specialization);
    }
}
