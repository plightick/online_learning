package com.example.online_learning.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void courseTaskExecutorShouldExposeConfiguredThreadPool() {
        ThreadPoolTaskExecutor executor = asyncConfig.courseTaskExecutor();

        assertEquals(4, executor.getCorePoolSize());
        assertEquals(8, executor.getMaxPoolSize());
        assertEquals(200, executor.getThreadPoolExecutor().getQueue().remainingCapacity());
        assertEquals("course-task-", executor.getThreadNamePrefix());

        executor.shutdown();
    }
}
