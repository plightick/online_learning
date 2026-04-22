package com.example.online_learning.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.online_learning.dto.RaceConditionDemoResponseDto;
import com.example.online_learning.dto.ThreadSafeCounterSnapshotDto;
import com.example.online_learning.dto.ThreadSafeCounterUpdateDto;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test
    void runIncrementBatchShouldSkipWorkWhenStartGateTimesOut() throws Exception {
        CountDownLatch startGate = mock(CountDownLatch.class);
        when(startGate.await(5, TimeUnit.SECONDS)).thenReturn(false);
        CountDownLatch finishGate = new CountDownLatch(1);
        Object unsafeCounter = newCounter("UnsafeCounter");
        AtomicInteger atomicCounter = new AtomicInteger();
        Object synchronizedCounter = newCounter("SynchronizedCounter");

        invokeRunIncrementBatch(5, startGate, finishGate, unsafeCounter, atomicCounter, synchronizedCounter);

        assertEquals(0L, finishGate.getCount());
        assertEquals(0, atomicCounter.get());
        assertEquals(0, readCounterValue(unsafeCounter));
        assertEquals(0, readCounterValue(synchronizedCounter));
    }

    @Test
    void runIncrementBatchShouldRestoreInterruptFlagWhenAwaitIsInterrupted() throws Exception {
        CountDownLatch startGate = mock(CountDownLatch.class);
        when(startGate.await(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException("boom"));
        CountDownLatch finishGate = new CountDownLatch(1);
        Object unsafeCounter = newCounter("UnsafeCounter");
        AtomicInteger atomicCounter = new AtomicInteger();
        Object synchronizedCounter = newCounter("SynchronizedCounter");

        try {
            invokeRunIncrementBatch(5, startGate, finishGate, unsafeCounter, atomicCounter, synchronizedCounter);

            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(0L, finishGate.getCount());
            assertEquals(0, atomicCounter.get());
            assertEquals(0, readCounterValue(unsafeCounter));
            assertEquals(0, readCounterValue(synchronizedCounter));
        } finally {
            Thread.interrupted();
        }
    }

    private void invokeRunIncrementBatch(
            int incrementsPerThread,
            CountDownLatch startGate,
            CountDownLatch finishGate,
            Object unsafeCounter,
            AtomicInteger atomicCounter,
            Object synchronizedCounter) throws Exception {
        Method method = ConcurrencyDemoService.class.getDeclaredMethod(
                "runIncrementBatch",
                int.class,
                CountDownLatch.class,
                CountDownLatch.class,
                unsafeCounter.getClass(),
                AtomicInteger.class,
                synchronizedCounter.getClass());
        method.setAccessible(true);
        method.invoke(
                service,
                incrementsPerThread,
                startGate,
                finishGate,
                unsafeCounter,
                atomicCounter,
                synchronizedCounter);
    }

    private static Object newCounter(String counterClassName) throws Exception {
        Class<?> counterClass = Class.forName(
                ConcurrencyDemoService.class.getName() + "$" + counterClassName);
        Constructor<?> constructor = counterClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static int readCounterValue(Object counter) throws Exception {
        Method method = counter.getClass().getDeclaredMethod("getValue");
        method.setAccessible(true);
        return (int) method.invoke(counter);
    }
}
