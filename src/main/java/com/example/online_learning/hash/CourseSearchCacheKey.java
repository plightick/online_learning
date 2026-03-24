package com.example.online_learning.hash;

import com.example.online_learning.dto.CourseSearchQueryType;
import java.util.Objects;

public final class CourseSearchCacheKey {

    private final String categoryName;
    private final String instructorSpecialization;
    private final int pageNumber;
    private final int pageSize;
    private final String sort;
    private final CourseSearchQueryType queryType;

    public CourseSearchCacheKey(
            String categoryName,
            String instructorSpecialization,
            int pageNumber,
            int pageSize,
            String sort,
            CourseSearchQueryType queryType) {
        this.categoryName = categoryName;
        this.instructorSpecialization = instructorSpecialization;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.sort = sort;
        this.queryType = queryType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CourseSearchCacheKey that)) {
            return false;
        }
        return pageNumber == that.pageNumber
                && pageSize == that.pageSize
                && Objects.equals(categoryName, that.categoryName)
                && Objects.equals(instructorSpecialization, that.instructorSpecialization)
                && Objects.equals(sort, that.sort)
                && queryType == that.queryType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                categoryName,
                instructorSpecialization,
                pageNumber,
                pageSize,
                sort,
                queryType);
    }
}
