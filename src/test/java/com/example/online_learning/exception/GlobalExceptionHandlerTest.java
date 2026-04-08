package com.example.online_learning.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.online_learning.exception.response.ErrorResponse;
import com.example.online_learning.exception.response.ValidationErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleApiExceptionShouldReturnConflictResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleApiException(new ConflictException("Duplicate"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertEquals("Duplicate", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleApiExceptionShouldReturnServerErrorResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleApiException(
                new TestApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertEquals("Boom", response.getBody().message());
    }

    @Test
    void handleMethodArgumentNotValidExceptionShouldCollectFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "title", "must not be blank"));
        bindingResult.addError(new FieldError("request", "level", "must not be blank"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                mock(MethodParameter.class),
                bindingResult);

        ResponseEntity<ValidationErrorResponse> response = handler.handleMethodArgumentNotValidException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("must not be blank", response.getBody().errors().get("title"));
        assertEquals("must not be blank", response.getBody().errors().get("level"));
    }

    @Test
    void handleConstraintViolationExceptionShouldUseLastNamedNodeOrRequestFallback() {
        ConstraintViolation<?> emailViolation = violation("must be a well-formed email address", "requestDto", "email");
        ConstraintViolation<?> requestViolation = violation("invalid request", (String) null);
        ConstraintViolationException exception = new ConstraintViolationException(
                Set.copyOf(new LinkedHashSet<>(List.of(emailViolation, requestViolation))));

        ResponseEntity<ValidationErrorResponse> response = handler.handleConstraintViolationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("must be a well-formed email address", response.getBody().errors().get("email"));
        assertEquals("invalid request", response.getBody().errors().get("request"));
    }

    @Test
    void handleMethodArgumentTypeMismatchExceptionShouldReturnBadRequest() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc",
                Long.class,
                "id",
                null,
                new IllegalArgumentException("bad value"));

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatchException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid value 'abc' for parameter 'id'", response.getBody().message());
    }

    @Test
    void handleHttpMessageNotReadableExceptionShouldReturnInvalidFieldMessage() {
        InvalidFormatException cause = InvalidFormatException.from(null, "bad value", "oops", String.class);
        cause.prependPath(new Object(), "level");
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Malformed JSON",
                cause,
                null);

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadableException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid value for field 'level'", response.getBody().message());
    }

    @Test
    void handleHttpMessageNotReadableExceptionShouldUseDefaultMessageForOtherCauses() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Malformed JSON",
                new IllegalArgumentException("broken payload"),
                null);

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadableException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed JSON request", response.getBody().message());
    }

    @Test
    void handleHttpMessageNotReadableExceptionShouldUseDefaultMessageWhenInvalidFormatPathIsNull() {
        InvalidFormatException cause = mock(InvalidFormatException.class);
        when(cause.getPath()).thenReturn(null);
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Malformed JSON",
                cause,
                null);

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadableException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed JSON request", response.getBody().message());
    }

    @Test
    void handleHttpMessageNotReadableExceptionShouldUseDefaultMessageWhenInvalidFormatPathIsEmpty() {
        InvalidFormatException cause = mock(InvalidFormatException.class);
        when(cause.getPath()).thenReturn(List.of());
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Malformed JSON",
                cause,
                null);

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadableException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed JSON request", response.getBody().message());
    }

    @Test
    void handleDataIntegrityViolationExceptionShouldReturnConflictForSpecificCause() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "duplicate key",
                new IllegalStateException("constraint violation"));

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Request conflicts with existing data", response.getBody().message());
    }

    @Test
    void handleDataIntegrityViolationExceptionShouldReturnConflictWithoutCause() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate key") {
            @Override
            public Throwable getMostSpecificCause() {
                return null;
            }
        };

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Request conflicts with existing data", response.getBody().message());
    }

    @Test
    void handleExceptionShouldReturnInternalServerError() {
        ResponseEntity<ErrorResponse> response = handler.handleException(new IllegalStateException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().message());
    }

    private static ConstraintViolation<Object> violation(String message, String... pathNodeNames) {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        List<Path.Node> nodes = java.util.Arrays.stream(pathNodeNames)
                .map(GlobalExceptionHandlerTest::node)
                .toList();
        when(path.iterator()).thenReturn(nodes.iterator());
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }

    private static Path.Node node(String name) {
        Path.Node node = mock(Path.Node.class);
        when(node.getName()).thenReturn(name);
        return node;
    }

    private static final class TestApiException extends ApiException {

        private TestApiException(HttpStatus status, String message) {
            super(status, message);
        }
    }
}
