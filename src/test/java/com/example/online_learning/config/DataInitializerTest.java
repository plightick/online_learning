package com.example.online_learning.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.online_learning.entity.Category;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Instructor;
import com.example.online_learning.entity.Student;
import com.example.online_learning.repository.CategoryRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.repository.StudentRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer();
    }

    @Test
    void seedDataShouldSkipWhenCoursesAlreadyExist() throws Exception {
        when(courseRepository.count()).thenReturn(1L);

        CommandLineRunner runner = dataInitializer.seedData(
                courseRepository,
                instructorRepository,
                studentRepository,
                categoryRepository);
        runner.run();

        verify(courseRepository).count();
        verify(studentRepository, never()).save(any(Student.class));
        verify(categoryRepository, never()).save(any(Category.class));
        verify(instructorRepository, never()).save(any(Instructor.class));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void seedDataShouldCreateDemoDataWhenDatabaseIsEmpty() throws Exception {
        when(courseRepository.count()).thenReturn(0L);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommandLineRunner runner = dataInitializer.seedData(
                courseRepository,
                instructorRepository,
                studentRepository,
                categoryRepository);
        runner.run();

        verify(studentRepository, times(3)).save(any(Student.class));
        verify(categoryRepository, times(3)).save(any(Category.class));
        verify(instructorRepository, times(2)).save(any(Instructor.class));

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository, times(2)).save(courseCaptor.capture());
        List<Course> savedCourses = courseCaptor.getAllValues();

        Course firstCourse = savedCourses.getFirst();
        assertEquals("Spring Boot Intensive", firstCourse.getTitle());
        assertEquals(2, firstCourse.getLessons().size());
        assertEquals(2, firstCourse.getStudents().size());
        assertEquals(2, firstCourse.getCategories().size());
        assertNotNull(firstCourse.getInstructor());

        Course secondCourse = savedCourses.get(1);
        assertEquals("PostgreSQL for Developers", secondCourse.getTitle());
        assertEquals(2, secondCourse.getLessons().size());
        assertEquals(2, secondCourse.getStudents().size());
        assertEquals(2, secondCourse.getCategories().size());
        assertTrue(secondCourse.getCategories().stream()
                .map(Category::getName)
                .anyMatch("DevOps"::equals));
    }
}
