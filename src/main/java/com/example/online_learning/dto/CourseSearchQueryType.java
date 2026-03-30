package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Query strategy used for course search.")
public enum CourseSearchQueryType {
    JPQL,
    NATIVE
}
