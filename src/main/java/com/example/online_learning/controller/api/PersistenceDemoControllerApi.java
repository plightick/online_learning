package com.example.online_learning.controller.api;

import com.example.online_learning.dto.RelatedSaveRequestDto;
import com.example.online_learning.dto.RelatedSaveResponseDto;
import com.example.online_learning.exception.response.ValidationErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Persistence Demo Controller", description = "Endpoints demonstrating transactional behavior")
public interface PersistenceDemoControllerApi {

    @Operation(
            summary = "Save without transaction",
            description = "Demonstrates partial persistence when an exception occurs without a transaction.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Demo executed successfully",
                content = @Content(schema = @Schema(implementation = RelatedSaveResponseDto.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @PostMapping("/api/demo/persistence/without-transaction")
    ResponseEntity<RelatedSaveResponseDto> saveWithoutTransaction(
            @Parameter(description = "Persistence demo payload", required = true)
            @Valid @RequestBody RelatedSaveRequestDto requestDto
    );

    @Operation(
            summary = "Save with transaction",
            description = "Demonstrates rollback when an exception occurs inside a transaction.")
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Demo executed successfully",
                content = @Content(schema = @Schema(implementation = RelatedSaveResponseDto.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @PostMapping("/api/demo/persistence/with-transaction")
    ResponseEntity<RelatedSaveResponseDto> saveWithTransaction(
            @Parameter(description = "Persistence demo payload", required = true)
            @Valid @RequestBody RelatedSaveRequestDto requestDto
    );
}
