package com.example.online_learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Asynchronous task state.")
public enum AsyncTaskState {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
