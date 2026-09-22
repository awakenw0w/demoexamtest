package ru.demoexam.algorithms.api;

/** Ответ, полученный от API. */
public record ApiResponse(
        ApiDataType dataType,
        String value
) {
}