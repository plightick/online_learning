package com.example.online_learning.controller;

import com.example.online_learning.controller.api.PersistenceDemoControllerApi;
import com.example.online_learning.dto.RelatedSaveRequestDto;
import com.example.online_learning.dto.RelatedSaveResponseDto;
import com.example.online_learning.service.PersistenceDemoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PersistenceDemoController implements PersistenceDemoControllerApi {

    private final PersistenceDemoService persistenceDemoService;

    public PersistenceDemoController(PersistenceDemoService persistenceDemoService) {
        this.persistenceDemoService = persistenceDemoService;
    }

    @PostMapping("/api/demo/persistence/without-transaction")
    public ResponseEntity<RelatedSaveResponseDto> saveWithoutTransaction(
            @Valid @RequestBody RelatedSaveRequestDto requestDto) {
        return ResponseEntity.ok(persistenceDemoService.saveWithoutTransaction(requestDto));
    }

    @PostMapping("/api/demo/persistence/with-transaction")
    public ResponseEntity<RelatedSaveResponseDto> saveWithTransaction(
            @Valid @RequestBody RelatedSaveRequestDto requestDto) {
        return ResponseEntity.ok(persistenceDemoService.saveWithTransaction(requestDto));
    }
}
