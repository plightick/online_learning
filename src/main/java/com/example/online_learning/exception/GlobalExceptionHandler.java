package com.example.online_learning.exception;

import com.example.online_learning.exception.response.ErrorResponse;
import com.example.online_learning.exception.response.ValidationErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_FAILED = "Validation failed";
    private static final String MALFORMED_JSON_REQUEST = "Malformed JSON request";
    private static final String UNEXPECTED_ERROR = "An unexpected error occurred";
    private static final String CONFLICTING_REQUEST = "Request conflicts with existing data";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        HttpStatus status = exception.getStatus();
        logApiException(status, exception);
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("{}: {}", VALIDATION_FAILED, errors);
        return ResponseEntity.badRequest()
                .body(buildValidationErrorResponse(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            errors.put(extractViolationName(violation), violation.getMessage());
        }

        log.warn("{}: {}", VALIDATION_FAILED, errors);
        return ResponseEntity.badRequest()
                .body(buildValidationErrorResponse(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception) {
        String message = "Invalid value '%s' for parameter '%s'"
                .formatted(exception.getValue(), exception.getName());
        log.warn("Method argument type mismatch: {}", message);
        return ResponseEntity.badRequest()
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = MALFORMED_JSON_REQUEST;
        if (cause instanceof InvalidFormatException invalidFormatException
                && invalidFormatException.getPath() != null
                && !invalidFormatException.getPath().isEmpty()) {
            String fieldName = invalidFormatException.getPath().getFirst().getFieldName();
            message = "Invalid value for field '%s'".formatted(fieldName);
        }
        log.warn("{}: {}", MALFORMED_JSON_REQUEST, message);
        return ResponseEntity.badRequest()
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String details = cause == null ? exception.getMessage() : cause.getMessage();
        log.warn("Data integrity violation: {}", details);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(HttpStatus.CONFLICT, CONFLICTING_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR));
    }

    private void logApiException(HttpStatus status, ApiException exception) {
        if (status.is5xxServerError()) {
            log.error("API exception [{}]: {}", status.value(), exception.getMessage());
            return;
        }
        log.warn("API exception [{}]: {}", status.value(), exception.getMessage());
    }

    private ErrorResponse buildErrorResponse(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), message, LocalDateTime.now());
    }

    private ValidationErrorResponse buildValidationErrorResponse(
            HttpStatus status,
            String message,
            Map<String, String> errors) {
        return new ValidationErrorResponse(status.value(), message, LocalDateTime.now(), errors);
    }

    private String extractViolationName(ConstraintViolation<?> violation) {
        String violationName = null;
        for (Path.Node node : violation.getPropertyPath()) {
            if (node.getName() != null) {
                violationName = node.getName();
            }
        }
        return violationName == null ? "request" : violationName;
    }
}
