package com.example.online_learning.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.online_learning.dto.RaceConditionDemoResponseDto;
import com.example.online_learning.dto.ThreadSafeCounterSnapshotDto;
import com.example.online_learning.dto.ThreadSafeCounterUpdateDto;
import com.example.online_learning.service.ConcurrencyDemoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ConcurrencyDemoControllerTest {

    @Mock
    private ConcurrencyDemoService concurrencyDemoService;

    private ConcurrencyDemoController controller;

    @BeforeEach
    void setUp() {
        controller = new ConcurrencyDemoController(concurrencyDemoService);
    }

    @Test
    void getCounterShouldReturnOkResponse() {
        ThreadSafeCounterSnapshotDto responseDto = new ThreadSafeCounterSnapshotDto("AtomicInteger", 5);
        when(concurrencyDemoService.getCounter()).thenReturn(responseDto);

        ResponseEntity<ThreadSafeCounterSnapshotDto> response = controller.getCounter();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseDto, response.getBody());
        verify(concurrencyDemoService).getCounter();
    }

    @Test
    void incrementCounterShouldReturnOkResponse() {
        ThreadSafeCounterUpdateDto responseDto = new ThreadSafeCounterUpdateDto("AtomicInteger", 5, 8, 3);
        when(concurrencyDemoService.incrementCounter(3)).thenReturn(responseDto);

        ResponseEntity<ThreadSafeCounterUpdateDto> response = controller.incrementCounter(3);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseDto, response.getBody());
        verify(concurrencyDemoService).incrementCounter(3);
    }

    @Test
    void resetCounterShouldReturnOkResponse() {
        ThreadSafeCounterSnapshotDto responseDto = new ThreadSafeCounterSnapshotDto("AtomicInteger", 0);
        when(concurrencyDemoService.resetCounter()).thenReturn(responseDto);

        ResponseEntity<ThreadSafeCounterSnapshotDto> response = controller.resetCounter();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseDto, response.getBody());
        verify(concurrencyDemoService).resetCounter();
    }

    @Test
    void runRaceConditionDemoShouldReturnOkResponse() {
        RaceConditionDemoResponseDto responseDto =
                new RaceConditionDemoResponseDto(64, 100, 6400, 5000, 6400, 6400, 1400);
        when(concurrencyDemoService.runRaceConditionDemo(64, 100)).thenReturn(responseDto);

        ResponseEntity<RaceConditionDemoResponseDto> response = controller.runRaceConditionDemo(64, 100);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(responseDto, response.getBody());
        verify(concurrencyDemoService).runRaceConditionDemo(64, 100);
    }
}
