package com.example.online_learning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.online_learning.cache.CourseSearchCacheInvalidator;
import com.example.online_learning.dto.StudentRequestDto;
import com.example.online_learning.dto.StudentResponseDto;
import com.example.online_learning.entity.Course;
import com.example.online_learning.entity.Student;
import com.example.online_learning.exception.DuplicateResourceException;
import com.example.online_learning.exception.ResourceNotFoundException;
import com.example.online_learning.mapper.StudentMapper;
import com.example.online_learning.repository.StudentRepository;
import java.util.List;
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
        StudentRequestDto requestDto = new StudentRequestDto("Anna", "Petrova", "anna@example.com");

        when(studentRepository.findByEmailIgnoreCase("anna@example.com"))
                .thenReturn(Optional.of(student(12L, "Anna", "Petrova", "anna@example.com")));

        assertThrows(
                DuplicateResourceException.class,
                () -> service.createStudent(requestDto));

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

    @Test
    void getStudentsShouldReturnMappedDtos() {
        Course algorithms = new Course("Algorithms", "ADVANCED");
        Course databases = new Course("Databases", "BEGINNER");
        Student student = student(9L, "Anna", "Petrova", "anna@example.com");
        student.getCourses().add(databases);
        student.getCourses().add(algorithms);
        when(studentRepository.findAll()).thenReturn(List.of(student));

        List<StudentResponseDto> responseDtos = service.getStudents();

        assertEquals(1, responseDtos.size());
        assertEquals(List.of("Algorithms", "Databases"), responseDtos.getFirst().enrolledCourses());
    }

    @Test
    void getStudentByIdShouldReturnMappedDto() {
        Student student = student(8L, "Oleg", "Sokolov", "oleg@example.com");
        when(studentRepository.findById(8L)).thenReturn(Optional.of(student));

        StudentResponseDto responseDto = service.getStudentById(8L);

        assertEquals(8L, responseDto.id());
        assertEquals("Oleg", responseDto.firstName());
    }

    @Test
    void getStudentByIdShouldThrowWhenMissing() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getStudentById(99L));

        assertTrue(exception.getMessage().contains("99"));
    }

    @Test
    void updateStudentShouldRejectEmailUsedByAnotherStudent() {
        Student currentStudent = student(5L, "Anna", "Petrova", "anna@example.com");
        Student otherStudent = student(6L, "Maria", "Green", "maria@example.com");
        when(studentRepository.findById(5L)).thenReturn(Optional.of(currentStudent));
        when(studentRepository.findByEmailIgnoreCase("maria@example.com"))
                .thenReturn(Optional.of(otherStudent));

        assertThrows(
                DuplicateResourceException.class,
                () -> service.updateStudent(
                        5L,
                        new StudentRequestDto("Anna", "Petrova", "maria@example.com")));

        verify(courseSearchCacheInvalidator, never()).invalidate();
    }

    @Test
    void updateStudentShouldThrowWhenStudentMissing() {
        when(studentRepository.findById(77L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateStudent(
                        77L,
                        new StudentRequestDto("Anna", "Petrova", "anna@example.com")));
    }

    @Test
    void deleteStudentShouldDetachCoursesDeleteAndInvalidateCache() {
        Student student = student(3L, "Anna", "Petrova", "anna@example.com");
        Course course = new Course("Spring", "ADVANCED");
        course.addStudent(student);
        student.getCourses().add(course);
        when(studentRepository.findById(3L)).thenReturn(Optional.of(student));

        service.deleteStudent(3L);

        assertTrue(course.getStudents().isEmpty());
        verify(studentRepository).delete(student);
        verify(courseSearchCacheInvalidator).invalidate();
    }

    @Test
    void deleteStudentShouldThrowWhenMissing() {
        when(studentRepository.findById(13L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteStudent(13L));
        verify(studentRepository, never()).delete(any(Student.class));
        verify(courseSearchCacheInvalidator, never()).invalidate();
    }

    private static Student student(Long id, String firstName, String lastName, String email) {
        Student student = new Student(firstName, lastName, email);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }
}
