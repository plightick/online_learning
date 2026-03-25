package com.example.online_learning.hash;

import com.example.online_learning.dto.CourseResponseDto;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class CourseSearchIndex {

    private static final int MAX_ENTRIES = 3;

    private final Map<CourseSearchCacheKey, Page<CourseResponseDto>> index =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<CourseSearchCacheKey, Page<CourseResponseDto>> eldest) {
                    return size() > MAX_ENTRIES;
                }
            };

    public synchronized Optional<Page<CourseResponseDto>> get(CourseSearchCacheKey key) {
        return Optional.ofNullable(index.get(key));
    }

    public synchronized void put(CourseSearchCacheKey key, Page<CourseResponseDto> value) {
        index.put(key, value);
    }

    public synchronized void clear() {
        index.clear();
    }

    public synchronized boolean contains(CourseSearchCacheKey key) {
        return index.containsKey(key);
    }

    public synchronized int size() {
        return index.size();
    }
}
