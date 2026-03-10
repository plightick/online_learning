package com.example.online_learning.controller;

import com.example.online_learning.dto.RelatedSaveRequestDto;
import com.example.online_learning.dto.RelatedSaveResponseDto;
import com.example.online_learning.service.PersistenceDemoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo/persistence")
public class PersistenceDemoController {

    private final PersistenceDemoService persistenceDemoService;

    public PersistenceDemoController(PersistenceDemoService persistenceDemoService) {
        this.persistenceDemoService = persistenceDemoService;
    }

    @PostMapping("/without-transaction")
    public RelatedSaveResponseDto saveWithoutTransaction(@Valid @RequestBody RelatedSaveRequestDto requestDto) {
        return persistenceDemoService.saveWithoutTransaction(requestDto);
    }

    @PostMapping("/with-transaction")
    public RelatedSaveResponseDto saveWithTransaction(@Valid @RequestBody RelatedSaveRequestDto requestDto) {
        return persistenceDemoService.saveWithTransaction(requestDto);
    }
}
