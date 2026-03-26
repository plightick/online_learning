package com.example.online_learning.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CourseSearchCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(CourseSearchCacheInvalidator.class);

    private final CourseSearchCache courseSearchCache;

    public CourseSearchCacheInvalidator(CourseSearchCache courseSearchCache) {
        this.courseSearchCache = courseSearchCache;
    }

    public void invalidate() {
        int cachedEntries = courseSearchCache.size();
        courseSearchCache.clear();
        log.info("Search cache invalidated: clearedEntries={}", cachedEntries);
    }
}
