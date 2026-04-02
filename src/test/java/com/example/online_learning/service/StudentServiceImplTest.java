package com.example.online_learning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.online_learning.cache.CourseSearchCacheInvalidator;
import com.example.online_learning.dto.StudentRequestDto;
import com.example.online_learning.dto.StudentResponseDto;
import com.example.online_learning.entity.Student;
import com.example.online_learning.exception.DuplicateResourceException;
import com.example.online_learning.mapper.StudentMapper;
import com.example.online_learning.repository.StudentRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseSearchCacheInvalidator courseSearchCacheInvalidator;

    private StudentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentServiceImpl(
                studentRepository,
                new StudentMapper(),
                courseSearchCacheInvalidator);
    }

    @Test
    void createStudentShouldPersistAndInvalidateCache() {
        when(studentRepository.findByEmailIgnoreCase("anna@example.com")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            ReflectionTestUtils.setField(student, "id", 7L);
            return student;
        });

        StudentResponseDto responseDto = service.createStudent(
                new StudentRequestDto("Anna", "Petrova", "anna@example.com"));

        assertEquals(7L, responseDto.id());
        assertEquals("Anna", responseDto.firstName());
        assertEquals("anna@example.com", responseDto.email());
        verify(studentRepository).save(any(Student.class));
        verify(courseSearchCacheInvalidator).invalidate();
    }

    @Test
    void createStudentShouldRejectDuplicateEmail() {
        when(studentRepository.findByEmailIgnoreCase("anna@example.com"))
                .thenReturn(Optional.of(student(12L, "Anna", "Petrova", "anna@example.com")));

        assertThrows(
                DuplicateResourceException.class,
                () -> service.createStudent(new StudentRequestDto("Anna", "Petrova", "anna@example.com")));

        verify(studentRepository, never()).save(any(Student.class));
        verify(courseSearchCacheInvalidator, never()).invalidate();
    }

    @Test
    void updateStudentShouldAllowKeepingCurrentEmail() {
        Student existingStudent = student(5L, "Anna", "Petrova", "anna@example.com");
        when(studentRepository.findById(5L)).thenReturn(Optional.of(existingStudent));
        when(studentRepository.findByEmailIgnoreCase("anna@example.com"))
                .thenReturn(Optional.of(existingStudent));

        StudentResponseDto responseDto = service.updateStudent(
                5L,
                new StudentRequestDto("Anastasia", "Petrova", "anna@example.com"));

        assertEquals(5L, responseDto.id());
        assertEquals("Anastasia", responseDto.firstName());
        assertEquals("anna@example.com", responseDto.email());
        verify(courseSearchCacheInvalidator).invalidate();
    }

    private static Student student(Long id, String firstName, String lastName, String email) {
        Student student = new Student(firstName, lastName, email);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }
}
