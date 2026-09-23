package ru.demoexam.algorithms.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Клиент для подключения к локальному TransferSimulator. */
public class TransferSimulatorClient {

    // Адрес локального API
    public static final String BASE_URL =
            "http://192.168.1.200:4444/TransferSimulator";

    // Поиск значения внутри JSON
    private static final Pattern VALUE_PATTERN =
            Pattern.compile("\\\"value\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");

    private final HttpClient httpClient;

    public TransferSimulatorClient() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    // Отправляет GET-запрос выбранного типа
    public CompletableFuture<ApiResponse> request(ApiDataType dataType) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        BASE_URL + "/" + dataType.endpoint()
                ))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return httpClient.sendAsync(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                )
                .thenApply(response ->
                        parseResponse(dataType, response)
                );
    }

    // Обрабатывает ответ сервера
    private ApiResponse parseResponse(
            ApiDataType dataType,
            HttpResponse<String> response
    ) {

        if (response.statusCode() != 200) {
            throw new ApiRequestException(response.statusCode());
        }

        Matcher matcher = VALUE_PATTERN.matcher(response.body());

        if (!matcher.find()) {
            throw new IllegalStateException(
                    "Сервер вернул ответ без поля value."
            );
        }

        String value = matcher.group(1)
                .replace("\\\\\"", "\"")
                .replace("\\\\\\\\", "\\");

        return new ApiResponse(dataType, value);
    }

    // Ошибка запроса к серверу
    public static class ApiRequestException extends RuntimeException {

        private final int statusCode;

        public ApiRequestException(int statusCode) {
            super("Сервер вернул HTTP " + statusCode + ".");
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }
}
