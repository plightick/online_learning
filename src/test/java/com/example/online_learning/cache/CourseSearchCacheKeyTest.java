package com.example.online_learning.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.online_learning.dto.CourseSearchQueryType;
import org.junit.jupiter.api.Test;

class CourseSearchCacheKeyTest {

    @Test
    void equalsAndHashCodeShouldUseAllFields() {
        CourseSearchCacheKey first = new CourseSearchCacheKey(
                "backend",
                "security",
                CourseSearchQueryType.JPQL,
                1,
                10);
        CourseSearchCacheKey same = new CourseSearchCacheKey(
                "backend",
                "security",
                CourseSearchQueryType.JPQL,
                1,
                10);
        CourseSearchCacheKey different = new CourseSearchCacheKey(
                "backend",
                "security",
                CourseSearchQueryType.NATIVE,
                1,
                10);

        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, different);
    }

    @Test
    void equalsShouldHandleNullDifferentTypeAndToString() {
        CourseSearchCacheKey key = new CourseSearchCacheKey(
                "backend",
                "security",
                CourseSearchQueryType.JPQL,
                2,
                20);

        assertNotEquals(key, null);
        assertNotEquals(key, "not-a-key");
        assertTrue(key.toString().contains("categoryName='backend'"));
        assertTrue(key.toString().contains("pageNumber=2"));
        assertTrue(key.toString().contains("pageSize=20"));
    }
}
