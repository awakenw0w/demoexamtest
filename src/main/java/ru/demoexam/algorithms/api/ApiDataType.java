package ru.demoexam.algorithms.api;

/** Types of data supported by the TransferSimulator API. */
public enum ApiDataType {
    FULL_NAME("ФИО клиента", "fullName"),
    SNILS("СНИЛС", "snils"),
    INN("ИНН", "inn"),
    EMAIL("Электронная почта", "email"),
    IDENTITY_CARD("Номер карты-пропуска", "identityCard");

    private final String title;
    private final String endpoint;

    ApiDataType(String title, String endpoint) {
        this.title = title;
        this.endpoint = endpoint;
    }

    public String endpoint() {
        return endpoint;
    }

    @Override
    public String toString() {
        return title;
    }
}
