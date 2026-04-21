package com.example.online_learning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.online_learning.dto.RaceConditionDemoResponseDto;
import com.example.online_learning.dto.ThreadSafeCounterSnapshotDto;
import com.example.online_learning.dto.ThreadSafeCounterUpdateDto;
import org.junit.jupiter.api.Test;

class ConcurrencyDemoServiceTest {

    private final ConcurrencyDemoService service = new ConcurrencyDemoService();

    @Test
    void incrementCounterShouldReturnPreviousAndCurrentValues() {
        ThreadSafeCounterUpdateDto response = service.incrementCounter(5);
        ThreadSafeCounterSnapshotDto snapshot = service.getCounter();

        assertEquals("AtomicInteger", response.strategy());
        assertEquals(0, response.previousValue());
        assertEquals(5, response.currentValue());
        assertEquals(5, response.incrementedBy());
        assertEquals(5, snapshot.value());
    }

    @Test
    void resetCounterShouldSetValueToZero() {
        service.incrementCounter(7);

        ThreadSafeCounterSnapshotDto response = service.resetCounter();

        assertEquals("AtomicInteger", response.strategy());
        assertEquals(0, response.value());
        assertEquals(0, service.getCounter().value());
    }

    @Test
    void runRaceConditionDemoShouldNormalizeThreadsAndKeepSafeCountersAccurate() {
        RaceConditionDemoResponseDto response = service.runRaceConditionDemo(10, 500);

        assertEquals(50, response.threads());
        assertEquals(25000, response.expectedTotal());
        assertEquals(response.expectedTotal(), response.atomicTotal());
        assertEquals(response.expectedTotal(), response.synchronizedTotal());
        assertTrue(response.unsafeTotal() < response.expectedTotal());
        assertEquals(response.expectedTotal() - response.unsafeTotal(), response.lostUpdates());
    }

    @Test
    void runRaceConditionDemoShouldFailWhenThreadIsInterrupted() {
        Thread.currentThread().interrupt();

        try {
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> service.runRaceConditionDemo(64, 5));

            assertEquals("Race condition demo was interrupted", exception.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
