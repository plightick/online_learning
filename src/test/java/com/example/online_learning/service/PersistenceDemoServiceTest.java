package com.example.online_learning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.online_learning.dto.LessonRequestDto;
import com.example.online_learning.dto.RelatedSaveRequestDto;
import com.example.online_learning.dto.RelatedSaveResponseDto;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Instructor;
import com.example.online_learning.exception.LoggingException;
import com.example.online_learning.repository.CourseRepository;
import com.example.online_learning.repository.InstructorRepository;
import com.example.online_learning.repository.LessonRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PersistenceDemoServiceTest {

    @Mock
    private RelatedPersistenceTransactionalWorker transactionalWorker;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    private PersistenceDemoService service;

    @BeforeEach
    void setUp() {
        service = new PersistenceDemoService(
                transactionalWorker,
                instructorRepository,
                courseRepository,
                lessonRepository);
    }

    @Test
    void saveWithoutTransactionShouldReturnPersistedIdsAfterFailure() {
        RelatedSaveRequestDto requestDto = requestDto();
        Instructor instructor = instructor(10L);
        Course course = course(20L, requestDto.courseTitle());

        doThrow(new IllegalStateException("Simulated failure after partial persistence"))
                .when(transactionalWorker)
                .persistScenario(requestDto);
        when(instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(
                requestDto.instructorFirstName(),
                requestDto.instructorLastName())).thenReturn(Optional.of(instructor));
        when(courseRepository.findByTitleIgnoreCase(requestDto.courseTitle())).thenReturn(Optional.of(course));
        when(lessonRepository.countByCourseTitleIgnoreCase(requestDto.courseTitle())).thenReturn(1L);

        RelatedSaveResponseDto responseDto = service.saveWithoutTransaction(requestDto);

        assertEquals("WITHOUT_TRANSACTION", responseDto.mode());
        assertEquals("Simulated failure after partial persistence", responseDto.message());
        assertEquals(10L, responseDto.instructorId());
        assertEquals(20L, responseDto.courseId());
        assertEquals(1L, responseDto.persistedLessons());
        verify(transactionalWorker).persistScenario(requestDto);
    }

    @Test
    void saveWithTransactionShouldUseCauseMessageFromLoggingException() {
        RelatedSaveRequestDto requestDto = requestDto();

        doThrow(new LoggingException("Wrapper message", new IllegalStateException("Inner failure")))
                .when(transactionalWorker)
                .saveWithRollback(requestDto);
        when(instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(
                requestDto.instructorFirstName(),
                requestDto.instructorLastName())).thenReturn(Optional.empty());
        when(courseRepository.findByTitleIgnoreCase(requestDto.courseTitle())).thenReturn(Optional.empty());
        when(lessonRepository.countByCourseTitleIgnoreCase(requestDto.courseTitle())).thenReturn(0L);

        RelatedSaveResponseDto responseDto = service.saveWithTransaction(requestDto);

        assertEquals("WITH_TRANSACTION", responseDto.mode());
        assertEquals("Inner failure", responseDto.message());
        assertNull(responseDto.instructorId());
        assertNull(responseDto.courseId());
        assertEquals(0L, responseDto.persistedLessons());
        verify(transactionalWorker).saveWithRollback(requestDto);
    }

    @Test
    void saveWithTransactionShouldUseOwnMessageWhenLoggingExceptionHasNoCause() {
        RelatedSaveRequestDto requestDto = requestDto();

        doThrow(new LoggingException("Wrapper message"))
                .when(transactionalWorker)
                .saveWithRollback(requestDto);
        when(instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(
                requestDto.instructorFirstName(),
                requestDto.instructorLastName())).thenReturn(Optional.empty());
        when(courseRepository.findByTitleIgnoreCase(requestDto.courseTitle())).thenReturn(Optional.empty());
        when(lessonRepository.countByCourseTitleIgnoreCase(requestDto.courseTitle())).thenReturn(0L);

        RelatedSaveResponseDto responseDto = service.saveWithTransaction(requestDto);

        assertEquals("WITH_TRANSACTION", responseDto.mode());
        assertEquals("Wrapper message", responseDto.message());
        assertEquals(0L, responseDto.persistedLessons());
    }

    @Test
    void saveWithoutTransactionShouldReturnUnexpectedSuccessWhenWorkerCompletes() {
        RelatedSaveRequestDto requestDto = requestDto();
        Instructor instructor = instructor(11L);
        Course course = course(21L, requestDto.courseTitle());
        when(instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(
                requestDto.instructorFirstName(),
                requestDto.instructorLastName())).thenReturn(Optional.of(instructor));
        when(courseRepository.findByTitleIgnoreCase(requestDto.courseTitle())).thenReturn(Optional.of(course));
        when(lessonRepository.countByCourseTitleIgnoreCase(requestDto.courseTitle())).thenReturn(1L);

        RelatedSaveResponseDto responseDto = service.saveWithoutTransaction(requestDto);

        assertEquals("WITHOUT_TRANSACTION", responseDto.mode());
        assertEquals("Unexpected success", responseDto.message());
        assertEquals(11L, responseDto.instructorId());
        assertEquals(21L, responseDto.courseId());
    }

    @Test
    void saveWithTransactionShouldReturnUnexpectedSuccessWhenWorkerCompletes() {
        RelatedSaveRequestDto requestDto = requestDto();
        Instructor instructor = instructor(12L);
        Course course = course(22L, requestDto.courseTitle());
        when(instructorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(
                requestDto.instructorFirstName(),
                requestDto.instructorLastName())).thenReturn(Optional.of(instructor));
        when(courseRepository.findByTitleIgnoreCase(requestDto.courseTitle())).thenReturn(Optional.of(course));
        when(lessonRepository.countByCourseTitleIgnoreCase(requestDto.courseTitle())).thenReturn(2L);

        RelatedSaveResponseDto responseDto = service.saveWithTransaction(requestDto);

        assertEquals("WITH_TRANSACTION", responseDto.mode());
        assertEquals("Unexpected success", responseDto.message());
        assertEquals(12L, responseDto.instructorId());
        assertEquals(22L, responseDto.courseId());
        assertEquals(2L, responseDto.persistedLessons());
    }

    private static RelatedSaveRequestDto requestDto() {
        return new RelatedSaveRequestDto(
                "Jane",
                "Brown",
                "Databases",
                "SQL Essentials",
                "Intermediate",
                List.of(new LessonRequestDto("Intro", 30, 1)),
                List.of("Databases"),
                List.of(1L));
    }

    private static Instructor instructor(Long id) {
        Instructor instructor = new Instructor("Jane", "Brown", "Databases");
        ReflectionTestUtils.setField(instructor, "id", id);
        return instructor;
    }

    private static Course course(Long id, String title) {
        Course course = new Course(title, "Intermediate");
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }
}
