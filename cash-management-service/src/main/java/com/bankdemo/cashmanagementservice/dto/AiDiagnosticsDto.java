package com.bankdemo.cashmanagementservice.dto;

public record AiDiagnosticsDto(
        boolean keyConfigured,
        String keyMasked,
        String model,
        String url,
        boolean success,
        Integer httpStatus,
        String errorBody,
        String errorMessage,
        String likelyCause) {}
