package com.example.online_learning.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PagedCourseIdsTest {

    @Test
    void recordShouldExposeConstructorValues() {
        PagedCourseIds pagedCourseIds = new PagedCourseIds(List.of(1L, 2L), 5L);

        assertEquals(List.of(1L, 2L), pagedCourseIds.courseIds());
        assertEquals(5L, pagedCourseIds.totalElements());
    }
}
