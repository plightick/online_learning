package com.example.online_learning.controller.api;

import com.example.online_learning.dto.StudentRequestDto;
import com.example.online_learning.dto.StudentResponseDto;
import com.example.online_learning.exception.response.ErrorResponse;
import com.example.online_learning.exception.response.ValidationErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Student Controller", description = "Student management endpoints")
public interface StudentControllerApi {

    @Operation(summary = "Create student", description = "Creates a new student.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "201",
                description = "Student created successfully",
                content = @Content(schema = @Schema(implementation = StudentResponseDto.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Student already exists",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/students")
    ResponseEntity<StudentResponseDto> createStudent(
            @Parameter(description = "Student payload", required = true)
            @Valid @RequestBody StudentRequestDto requestDto
    );

    @Operation(summary = "Get all students", description = "Returns all students.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Students retrieved successfully",
                content = @Content(
                        array = @ArraySchema(schema = @Schema(implementation = StudentResponseDto.class))))
    })
    @GetMapping("/api/students")
    ResponseEntity<List<StudentResponseDto>> getStudents();

    @Operation(summary = "Get student by ID", description = "Returns student details by identifier.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Student found",
                content = @Content(schema = @Schema(implementation = StudentResponseDto.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/students/{id}")
    ResponseEntity<StudentResponseDto> getStudentById(
            @Parameter(description = "Student ID", required = true, example = "1")
            @PathVariable Long id
    );

    @Operation(summary = "Update student", description = "Updates an existing student.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Student updated successfully",
                content = @Content(schema = @Schema(implementation = StudentResponseDto.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Student not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Student already exists",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/api/students/{id}")
    ResponseEntity<StudentResponseDto> updateStudent(
            @Parameter(description = "Student ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Updated student payload", required = true)
            @Valid @RequestBody StudentRequestDto requestDto
    );

    @Operation(summary = "Delete student", description = "Deletes a student by ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Student deleted successfully"),
        @ApiResponse(
                responseCode = "404",
                description = "Student not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/students/{id}")
    ResponseEntity<Void> deleteStudent(
            @Parameter(description = "Student ID", required = true, example = "1")
            @PathVariable Long id
    );
}
