package com.example.online_learning.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.online_learning.dto.StudentRequestDto;
import com.example.online_learning.dto.StudentResponseDto;
import com.example.online_learning.service.StudentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock
    private StudentService studentService;

    private StudentController controller;

    @BeforeEach
    void setUp() {
        controller = new StudentController(studentService);
    }

    @Test
    void createStudentShouldReturnCreatedResponse() {
        StudentRequestDto requestDto = requestDto("Anna");
        StudentResponseDto responseDto = responseDto(1L, "Anna");
        when(studentService.createStudent(requestDto)).thenReturn(responseDto);

        ResponseEntity<StudentResponseDto> response = controller.createStudent(requestDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(responseDto, response.getBody());
        verify(studentService).createStudent(requestDto);
    }

    @Test
    void getStudentsShouldReturnBody() {
        List<StudentResponseDto> responseDtos = List.of(responseDto(2L, "Alex"));
        when(studentService.getStudents()).thenReturn(responseDtos);

        ResponseEntity<List<StudentResponseDto>> response = controller.getStudents();

        assertSame(responseDtos, response.getBody());
        verify(studentService).getStudents();
    }

    @Test
    void getStudentByIdShouldReturnBody() {
        StudentResponseDto responseDto = responseDto(3L, "Mila");
        when(studentService.getStudentById(3L)).thenReturn(responseDto);

        ResponseEntity<StudentResponseDto> response = controller.getStudentById(3L);

        assertSame(responseDto, response.getBody());
        verify(studentService).getStudentById(3L);
    }

    @Test
    void updateStudentShouldReturnBody() {
        StudentRequestDto requestDto = requestDto("Nina");
        StudentResponseDto responseDto = responseDto(4L, "Nina");
        when(studentService.updateStudent(4L, requestDto)).thenReturn(responseDto);

        ResponseEntity<StudentResponseDto> response = controller.updateStudent(4L, requestDto);

        assertSame(responseDto, response.getBody());
        verify(studentService).updateStudent(4L, requestDto);
    }

    @Test
    void deleteStudentShouldReturnNoContent() {
        ResponseEntity<Void> response = controller.deleteStudent(5L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(studentService).deleteStudent(5L);
    }

    private static StudentRequestDto requestDto(String firstName) {
        return new StudentRequestDto(firstName, "Brown", firstName.toLowerCase() + "@example.com");
    }

    private static StudentResponseDto responseDto(Long id, String firstName) {
        return new StudentResponseDto(
                id,
                firstName,
                "Brown",
                firstName.toLowerCase() + "@example.com",
                List.of());
    }
}
