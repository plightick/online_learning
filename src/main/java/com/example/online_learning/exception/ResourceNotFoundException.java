package com.example.online_learning.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        this(resourceName + " with id " + id + " was not found");
    }
}
