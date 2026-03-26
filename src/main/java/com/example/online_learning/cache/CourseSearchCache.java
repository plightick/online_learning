package com.example.online_learning.cache;

import com.example.online_learning.dto.CourseResponseDto;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface CourseSearchCache {

    Optional<Page<CourseResponseDto>> get(CourseSearchCacheKey key);

    void put(CourseSearchCacheKey key, Page<CourseResponseDto> value);

    void clear();

    boolean contains(CourseSearchCacheKey key);

    int size();
}
