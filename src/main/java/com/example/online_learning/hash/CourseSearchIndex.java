package com.example.online_learning.hash;

import com.example.online_learning.dto.CourseResponseDto;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class CourseSearchIndex {

    private final Map<CourseSearchCacheKey, Page<CourseResponseDto>> index = new HashMap<>();

    public synchronized Optional<Page<CourseResponseDto>> get(CourseSearchCacheKey key) {
        return Optional.ofNullable(index.get(key));
    }

    public synchronized void put(CourseSearchCacheKey key, Page<CourseResponseDto> value) {
        index.put(key, value);
    }

    public synchronized void clear() {
        index.clear();
    }
}
