package com.example.online_learning.service;

import com.example.online_learning.dto.RaceConditionDemoResponseDto;
import com.example.online_learning.dto.ThreadSafeCounterSnapshotDto;
import com.example.online_learning.dto.ThreadSafeCounterUpdateDto;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.springframework.stereotype.Service;

@Service
public class ConcurrencyDemoService {

    private static final String COUNTER_STRATEGY = "AtomicInteger";

    private final AtomicInteger threadSafeCounter = new AtomicInteger();

    public ThreadSafeCounterSnapshotDto getCounter() {
        return new ThreadSafeCounterSnapshotDto(COUNTER_STRATEGY, threadSafeCounter.get());
    }

    public ThreadSafeCounterUpdateDto incrementCounter(int times) {
        int previousValue = threadSafeCounter.getAndAdd(times);
        return new ThreadSafeCounterUpdateDto(
                COUNTER_STRATEGY,
                previousValue,
                previousValue + times,
                times);
    }

    public ThreadSafeCounterSnapshotDto resetCounter() {
        threadSafeCounter.set(0);
        return new ThreadSafeCounterSnapshotDto(COUNTER_STRATEGY, 0);
    }

    public RaceConditionDemoResponseDto runRaceConditionDemo(int threads, int incrementsPerThread) {
        int normalizedThreads = Math.max(threads, 50);
        UnsafeCounter unsafeCounter = new UnsafeCounter();
        AtomicInteger atomicCounter = new AtomicInteger();
        SynchronizedCounter synchronizedCounter = new SynchronizedCounter();
        ExecutorService executorService = Executors.newFixedThreadPool(normalizedThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(normalizedThreads);

        try {
            for (int threadIndex = 0; threadIndex < normalizedThreads; threadIndex++) {
                executorService.execute(() -> runIncrementBatch(
                        incrementsPerThread,
                        startGate,
                        finishGate,
                        unsafeCounter,
                        atomicCounter,
                        synchronizedCounter));
            }

            startGate.countDown();
            finishGate.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Race condition demo was interrupted", exception);
        } finally {
            executorService.shutdownNow();
        }

        int expectedTotal = normalizedThreads * incrementsPerThread;
        int unsafeTotal = unsafeCounter.getValue();
        return new RaceConditionDemoResponseDto(
                normalizedThreads,
                incrementsPerThread,
                expectedTotal,
                unsafeTotal,
                atomicCounter.get(),
                synchronizedCounter.getValue(),
                expectedTotal - unsafeTotal);
    }

    private void runIncrementBatch(
            int incrementsPerThread,
            CountDownLatch startGate,
            CountDownLatch finishGate,
            UnsafeCounter unsafeCounter,
            AtomicInteger atomicCounter,
            SynchronizedCounter synchronizedCounter) {
        try {
            startGate.await(5, TimeUnit.SECONDS);
            for (int increment = 0; increment < incrementsPerThread; increment++) {
                unsafeCounter.increment();
                atomicCounter.incrementAndGet();
                synchronizedCounter.increment();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            finishGate.countDown();
        }
    }

    private static final class UnsafeCounter {

        private int value;

        private void increment() {
            value++;
        }

        private int getValue() {
            return value;
        }
    }

    private static final class SynchronizedCounter {

        private int value;

        private synchronized void increment() {
            value++;
        }

        private synchronized int getValue() {
            return value;
        }
    }
}
