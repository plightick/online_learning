package com.example.online_learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.online_learning.cache.CourseSearchCacheInvalidator;
import com.example.online_learning.dto.LessonRequestDto;
import com.example.online_learning.dto.RelatedSaveRequestDto;
import com.example.online_learning.entity.Category;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Instructor;
import com.example.online_learning.entity.Lesson;
import com.example.online_learning.entity.Student;
import com.example.online_learning.exception.ResourceNotFoundException;
import com.example.online_learning.repository.CategoryRepository;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.repository.LessonRepository;
import com.example.online_learning.repository.StudentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RelatedPersistenceTransactionalWorkerTest {

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseSearchCacheInvalidator courseSearchCacheInvalidator;

    private RelatedPersistenceTransactionalWorker worker;

    @BeforeEach
    void setUp() {
        worker = new RelatedPersistenceTransactionalWorker(
                instructorRepository,
                courseRepository,
                lessonRepository,
                categoryRepository,
                studentRepository,
                courseSearchCacheInvalidator);
    }

    @Test
    void saveWithRollbackShouldPersistRelationsAndInvalidateCache() {
        RelatedSaveRequestDto requestDto = requestDto(List.of("Backend", "Database"), List.of(1L));
        Instructor instructor = new Instructor("Jane", "Brown", "Databases");
        Category existingCategory = new Category("Backend");
        Category newCategory = new Category("Database");
        Student student = student(1L);
        when(instructorRepository.save(any(Instructor.class))).thenReturn(instructor);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.findByNameIgnoreCase("Backend")).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.findByNameIgnoreCase("Database")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> worker.saveWithRollback(requestDto));

        assertEquals("Simulated failure after partial persistence", exception.getMessage());
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository, org.mockito.Mockito.times(2)).save(courseCaptor.capture());
        Course persistedCourse = courseCaptor.getAllValues().getLast();
        assertThat(persistedCourse.getInstructor()).isEqualTo(instructor);
        assertEquals(2, persistedCourse.getCategories().size());
        assertEquals(1, persistedCourse.getStudents().size());

        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository).save(lessonCaptor.capture());
        assertThat(lessonCaptor.getValue().getCourse()).isEqualTo(persistedCourse);
        verify(courseSearchCacheInvalidator).invalidate();
    }

    @Test
    void saveWithRollbackShouldDelegateToPersistScenario() {
        RelatedSaveRequestDto requestDto = requestDto(null, null);
        RelatedPersistenceTransactionalWorker workerSpy = spy(worker);
        doNothing().when(workerSpy).persistScenario(requestDto);

        workerSpy.saveWithRollback(requestDto);

        verify(workerSpy).persistScenario(requestDto);
    }

    @Test
    void persistScenarioShouldSkipSharedRelationsWhenListsAreNull() {
        RelatedSaveRequestDto requestDto = requestDto(null, null);
        when(instructorRepository.save(any(Instructor.class)))
                .thenReturn(new Instructor("Jane", "Brown", "Databases"));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> worker.persistScenario(requestDto));

        assertTrue(exception.getMessage().contains("Simulated failure"));
        verifyNoInteractions(categoryRepository, studentRepository);
        verify(courseRepository, org.mockito.Mockito.times(2)).save(any(Course.class));
        verify(lessonRepository).save(any(Lesson.class));
        verify(courseSearchCacheInvalidator).invalidate();
    }

    @Test
    void persistScenarioShouldThrowWhenStudentIsMissingAndStillInvalidateCache() {
        RelatedSaveRequestDto requestDto = requestDto(List.of("Backend"), List.of(99L));
        when(instructorRepository.save(any(Instructor.class)))
                .thenReturn(new Instructor("Jane", "Brown", "Databases"));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.findByNameIgnoreCase("Backend")).thenReturn(Optional.of(new Category("Backend")));
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> worker.persistScenario(requestDto));

        assertTrue(exception.getMessage().contains("99"));
        verify(courseRepository).save(any(Course.class));
        verify(lessonRepository, never()).save(any(Lesson.class));
        verify(courseSearchCacheInvalidator).invalidate();
    }

    private static RelatedSaveRequestDto requestDto(List<String> categories, List<Long> studentIds) {
        return new RelatedSaveRequestDto(
                "Jane",
                "Brown",
                "Databases",
                "SQL Essentials",
                "INTERMEDIATE",
                List.of(new LessonRequestDto("Intro", 30, 1)),
                categories,
                studentIds);
    }

    private static Student student(Long id) {
        Student student = new Student("Anna", "Petrova", "anna@example.com");
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }
}
