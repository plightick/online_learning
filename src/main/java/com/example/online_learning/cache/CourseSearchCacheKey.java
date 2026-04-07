package com.example.online_learning.cache;

import com.example.online_learning.dto.CourseSearchQueryType;
import java.util.Objects;

public final class CourseSearchCacheKey {

    private final String categoryName;
    private final String instructorSpecialization;
    private final CourseSearchQueryType queryType;
    private final int pageNumber;
    private final int pageSize;

    public CourseSearchCacheKey(
            String categoryName,
            String instructorSpecialization,
            CourseSearchQueryType queryType,
            int pageNumber,
            int pageSize) {
        this.categoryName = categoryName;
        this.instructorSpecialization = instructorSpecialization;
        this.queryType = queryType;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CourseSearchCacheKey that)) {
            return false;
        }
        if (!Objects.equals(categoryName, that.categoryName)) {
            return false;
        }
        if (!Objects.equals(instructorSpecialization, that.instructorSpecialization)) {
            return false;
        }
        if (queryType != that.queryType) {
            return false;
        }
        if (pageNumber != that.pageNumber) {
            return false;
        }
        return pageSize == that.pageSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryName, instructorSpecialization, queryType, pageNumber, pageSize);
    }

    @Override
    public String toString() {
        return "CourseSearchCacheKey{"
                + "categoryName='" + categoryName + '\''
                + ", instructorSpecialization='" + instructorSpecialization + '\''
                + ", queryType=" + queryType
                + ", pageNumber=" + pageNumber
                + ", pageSize=" + pageSize
                + '}';
    }
}
