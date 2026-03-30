package com.example.online_learning.controller;

import com.example.online_learning.controller.api.StudentControllerApi;
import com.example.online_learning.dto.StudentRequestDto;
import com.example.online_learning.dto.StudentResponseDto;
import com.example.online_learning.service.StudentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentController implements StudentControllerApi {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/api/students")
    public ResponseEntity<StudentResponseDto> createStudent(@Valid @RequestBody StudentRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentService.createStudent(requestDto));
    }

    @GetMapping("/api/students")
    public ResponseEntity<List<StudentResponseDto>> getStudents() {
        return ResponseEntity.ok(studentService.getStudents());
    }

    @GetMapping("/api/students/{id:\\d+}")
    public ResponseEntity<StudentResponseDto> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @PutMapping("/api/students/{id:\\d+}")
    public ResponseEntity<StudentResponseDto> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDto requestDto) {
        return ResponseEntity.ok(studentService.updateStudent(id, requestDto));
    }

    @DeleteMapping("/api/students/{id:\\d+}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
