package com.example.online_learning.controller.api;

import com.example.online_learning.dto.RaceConditionDemoResponseDto;
import com.example.online_learning.dto.ThreadSafeCounterSnapshotDto;
import com.example.online_learning.dto.ThreadSafeCounterUpdateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
        name = "Concurrency Demo",
        description = "Thread-safe counter and race condition demonstration endpoints.")
public interface ConcurrencyDemoControllerApi {

    @Operation(summary = "Get thread-safe counter", description = "Returns the current AtomicInteger value.")
    @ApiResponse(responseCode = "200", description = "Counter value returned")
    @GetMapping("/api/concurrency/counter")
    ResponseEntity<ThreadSafeCounterSnapshotDto> getCounter();

    @Operation(
            summary = "Increment thread-safe counter",
            description = "Increments the AtomicInteger-based counter by a specified positive amount.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Counter updated"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid increment value",
                useReturnTypeSchema = true)
    })
    @PostMapping("/api/concurrency/counter/increment")
    ResponseEntity<ThreadSafeCounterUpdateDto> incrementCounter(
            @Parameter(description = "How much to add to the counter", required = true, example = "5")
            @RequestParam(defaultValue = "1") @Positive int times
    );

    @Operation(summary = "Reset thread-safe counter", description = "Sets the AtomicInteger counter back to zero.")
    @ApiResponse(responseCode = "200", description = "Counter reset")
    @DeleteMapping("/api/concurrency/counter")
    ResponseEntity<ThreadSafeCounterSnapshotDto> resetCounter();

    @Operation(
            summary = "Run race condition demo",
            description = "Starts 50+ threads and compares an unsafe counter "
                    + "with AtomicInteger and synchronized solutions.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Demo completed"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid query parameters",
                useReturnTypeSchema = true)
    })
    @PostMapping("/api/concurrency/race-condition-demo")
    ResponseEntity<RaceConditionDemoResponseDto> runRaceConditionDemo(
            @Parameter(
                    description = "Requested number of threads. Values below 50 are normalized to 50.",
                    example = "64")
            @RequestParam(defaultValue = "64") @Positive int threads,
            @Parameter(description = "How many increments each thread performs", example = "2000")
            @RequestParam(defaultValue = "2000") @Positive int incrementsPerThread
    );
}
