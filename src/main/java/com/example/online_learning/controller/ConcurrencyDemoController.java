package com.example.online_learning.controller;

import com.example.online_learning.controller.api.ConcurrencyDemoControllerApi;
import com.example.online_learning.dto.RaceConditionDemoResponseDto;
import com.example.online_learning.dto.ThreadSafeCounterSnapshotDto;
import com.example.online_learning.dto.ThreadSafeCounterUpdateDto;
import com.example.online_learning.service.ConcurrencyDemoService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class ConcurrencyDemoController implements ConcurrencyDemoControllerApi {

    private final ConcurrencyDemoService concurrencyDemoService;

    public ConcurrencyDemoController(ConcurrencyDemoService concurrencyDemoService) {
        this.concurrencyDemoService = concurrencyDemoService;
    }

    @Override
    public ResponseEntity<ThreadSafeCounterSnapshotDto> getCounter() {
        return ResponseEntity.ok(concurrencyDemoService.getCounter());
    }

    @Override
    public ResponseEntity<ThreadSafeCounterUpdateDto> incrementCounter(int times) {
        return ResponseEntity.ok(concurrencyDemoService.incrementCounter(times));
    }

    @Override
    public ResponseEntity<ThreadSafeCounterSnapshotDto> resetCounter() {
        return ResponseEntity.ok(concurrencyDemoService.resetCounter());
    }

    @Override
    public ResponseEntity<RaceConditionDemoResponseDto> runRaceConditionDemo(int threads, int incrementsPerThread) {
        return ResponseEntity.ok(concurrencyDemoService.runRaceConditionDemo(threads, incrementsPerThread));
    }
}
