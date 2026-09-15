package ru.demoexam.algorithms.api;

/** Value returned by a TransferSimulator endpoint. */
public record ApiResponse(ApiDataType dataType, String value) {
}
