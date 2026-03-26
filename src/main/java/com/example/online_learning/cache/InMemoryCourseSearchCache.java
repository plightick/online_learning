package com.example.online_learning.cache;

import com.example.online_learning.dto.CourseResponseDto;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class InMemoryCourseSearchCache implements CourseSearchCache {

    private static final int MAX_ENTRIES = 3;

    private final Map<CourseSearchCacheKey, Page<CourseResponseDto>> cache =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<CourseSearchCacheKey, Page<CourseResponseDto>> eldest) {
                    return super.size() > MAX_ENTRIES;
                }
            };

    @Override
    public synchronized Optional<Page<CourseResponseDto>> get(CourseSearchCacheKey key) {
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public synchronized void put(CourseSearchCacheKey key, Page<CourseResponseDto> value) {
        cache.put(key, value);
    }

    @Override
    public synchronized void clear() {
        cache.clear();
    }

    @Override
    public synchronized boolean contains(CourseSearchCacheKey key) {
        return cache.containsKey(key);
    }

    @Override
    public synchronized int size() {
        return cache.size();
    }
}
