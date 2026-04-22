package com.example.online_learning.cache;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(first).isEqualTo(first);
        assertThat(same)
                .isEqualTo(first)
                .hasSameHashCodeAs(first);
        assertThat(first).isNotEqualTo(different);
    }

    @Test
    void equalsShouldHandleNullDifferentTypeAndToString() {
        CourseSearchCacheKey key = new CourseSearchCacheKey(
                "backend",
                "security",
                CourseSearchQueryType.JPQL,
                2,
                20);
        Object unrelatedObject = new Object();

        assertThat(key)
                .isNotEqualTo(null)
                .isNotEqualTo(unrelatedObject);
        assertThat(key.toString())
                .contains("categoryName='backend'")
                .contains("pageNumber=2")
                .contains("pageSize=20");
    }

    @Test
    void equalsShouldDetectDifferencesAcrossEveryField() {
        CourseSearchCacheKey base = new CourseSearchCacheKey(
                "backend",
                "security",
                CourseSearchQueryType.JPQL,
                1,
                10);

        assertThat(base)
                .isNotEqualTo(new CourseSearchCacheKey(
                        "frontend",
                        "security",
                        CourseSearchQueryType.JPQL,
                        1,
                        10))
                .isNotEqualTo(new CourseSearchCacheKey(
                        "backend",
                        "platform",
                        CourseSearchQueryType.JPQL,
                        1,
                        10))
                .isNotEqualTo(new CourseSearchCacheKey(
                        "backend",
                        "security",
                        CourseSearchQueryType.JPQL,
                        2,
                        10))
                .isNotEqualTo(new CourseSearchCacheKey(
                        "backend",
                        "security",
                        CourseSearchQueryType.JPQL,
                        1,
                        20));
    }
}
